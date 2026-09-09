
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import delete, select, text
from datetime import datetime, timedelta, timezone

from app.database import engine, SessionLocal
from app.models import (
    Order,
    OrderItem,
    POS,
    POSInventory,
    Product,
    ProductInventory,
    Reservation,
    ReservationItem,
)


app = FastAPI(title="Automatic Brocolli Cat Backend")


app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# REQUEST MODELS


class OrderItemRequest(BaseModel):
    product_id: int
    quantity: int


class OrderRequest(BaseModel):
    pos_id: int
    items: list[OrderItemRequest]

class ReservationItemRequest(BaseModel):
    product_id: int
    quantity: int


class ReservationRequest(BaseModel):
    pos_id: int
    items: list[ReservationItemRequest]

class SimulatedPaymentRequest(BaseModel):
    reservation_id: int
    success: bool

# BASIC / HEALTH

@app.get("/")
def root():
    return {"status": "alive"}


@app.get("/health/db")
def database_health():
    with engine.connect() as connection:
        result = connection.execute(text("SELECT 1"))
        value = result.scalar()

    return {
        "database": "connected",
        "result": value,
    }


# PRODUCTS

@app.get("/products")
def get_products():
    with SessionLocal() as session:
        products = session.scalars(
            select(Product).order_by(Product.id)
        ).all()

        return [
            {
                "id": product.id,
                "uuid": str(product.uuid),
                "name": product.name,
                "brand": product.brand,
                "mrp": product.mrp,
                "unit": product.unit,
            }
            for product in products
        ]


@app.get("/products/{product_id}/inventory")
def get_product_inventory(product_id: int):
    with SessionLocal() as session:
        release_expired_reservations(session)
        session.commit()
        product = session.get(Product, product_id)

        if product is None:
            raise HTTPException(
                status_code=404,
                detail="Product not found",
            )

        global_inventory = session.scalar(
            select(ProductInventory).where(
                ProductInventory.product_id == product_id
            )
        )

        pos_rows = session.scalars(
            select(POSInventory)
            .where(POSInventory.product_id == product_id)
            .order_by(POSInventory.pos_id)
        ).all()

        return {
            "product_id": product.id,
            "physical_stock": (
                global_inventory.physical_stock
                if global_inventory
                else 0
            ),
            "pos": [
                {
                    "pos_id": row.pos_id,
                    "allocated_stock": row.allocated_stock,
                    "blocked_stock": row.blocked_stock,
                    "available_stock": (
                        row.allocated_stock - row.blocked_stock
                    ),
                }
                for row in pos_rows
            ],
        }


# DEVELOPMENT CONTROLS

@app.post("/dev/reset")
def reset_database(seed: int | None = None):
    import random
    from sqlalchemy import text

    MIN_STOCK = 10
    MAX_STOCK = 1000

    rng = random.Random(seed)

    with SessionLocal() as session:
       
        session.execute(delete(OrderItem))
        session.execute(delete(Order))

        session.execute(
        text("ALTER SEQUENCE orders_id_seq RESTART WITH 1")
        )
        session.execute(
        text("ALTER SEQUENCE order_items_id_seq RESTART WITH 1")
        )

        
        session.execute(delete(ReservationItem))
        session.execute(delete(Reservation))

        session.execute(delete(POSInventory))
        session.execute(delete(ProductInventory))

        products = session.scalars(
            select(Product).order_by(Product.id)
        ).all()

        poses = session.scalars(
            select(POS).order_by(POS.id)
        ).all()

        if not poses:
            raise HTTPException(
                status_code=500,
                detail="No POS terminals found.",
            )

        pos_count = len(poses)

        for product in products:
            stock = rng.randint(MIN_STOCK, MAX_STOCK)

            session.add(
                ProductInventory(
                    product_id=product.id,
                    physical_stock=stock,
                    version=0,
                )
            )

            base = stock // pos_count
            remainder = stock % pos_count

            for index, pos in enumerate(poses):
                allocation = base + (1 if index < remainder else 0)

                session.add(
                    POSInventory(
                        pos_id=pos.id,
                        product_id=product.id,
                        allocated_stock=allocation,
                        blocked_stock=0,
                    )
                )

        session.commit()

    return {
        "status": "reset",
        "seed": seed,
        "products": len(products),
        "pos": len(poses),
    }

def release_expired_reservations(session):
    now = datetime.now(timezone.utc)

    expired = session.scalars(
        select(Reservation)
        .where(
            Reservation.status == "PENDING",
            Reservation.expires_at <= now,
        )
        .with_for_update()
    ).all()

    released = []

    for reservation in expired:
        items = session.scalars(
            select(ReservationItem)
            .where(
                ReservationItem.reservation_id == reservation.id
            )
        ).all()

        for item in items:
            pos_inventory = session.scalar(
                select(POSInventory)
                .where(
                    POSInventory.pos_id == reservation.pos_id,
                    POSInventory.product_id == item.product_id,
                )
                .with_for_update()
            )

            if pos_inventory is not None:
                pos_inventory.blocked_stock = max(
                    0,
                    pos_inventory.blocked_stock - item.quantity,
                )

        released.append(reservation.id)

        session.execute(
            delete(ReservationItem).where(
                ReservationItem.reservation_id == reservation.id
            )
        )

        session.delete(reservation)

    return released
@app.post("/reservations")
def create_reservation(reservation: ReservationRequest):
    if not reservation.items:
        raise HTTPException(
            status_code=400,
            detail="Reservation must contain at least one item.",
        )

    with SessionLocal() as session:
        try:
            # Clean up expired holds before checking availability.
            release_expired_reservations(session)

            expires_at = (
                datetime.now(timezone.utc)
                + timedelta(minutes=5)
            )

            new_reservation = Reservation(
                pos_id=reservation.pos_id,
                status="PENDING",
                expires_at=expires_at,
            )

            session.add(new_reservation)
            session.flush()

            results = []

            for item in reservation.items:
                if item.quantity <= 0:
                    raise HTTPException(
                        status_code=400,
                        detail="Quantity must be positive.",
                    )

                pos_inventory = session.scalar(
                    select(POSInventory)
                    .where(
                        POSInventory.pos_id == reservation.pos_id,
                        POSInventory.product_id == item.product_id,
                    )
                    .with_for_update()
                )

                if pos_inventory is None:
                    raise HTTPException(
                        status_code=404,
                        detail=(
                            f"No inventory allocation for POS "
                            f"{reservation.pos_id}, product {item.product_id}."
                        ),
                    )

                available_stock = (
                    pos_inventory.allocated_stock
                    - pos_inventory.blocked_stock
                )

                if available_stock < item.quantity:
                    raise HTTPException(
                        status_code=409,
                        detail=(
                            f"Insufficient available stock for product "
                            f"{item.product_id}. "
                            f"Available: {available_stock}, "
                            f"requested: {item.quantity}."
                        ),
                    )

                pos_inventory.blocked_stock += item.quantity

                session.add(
                    ReservationItem(
                        reservation_id=new_reservation.id,
                        product_id=item.product_id,
                        quantity=item.quantity,
                    )
                )

                results.append(
                    {
                        "product_id": item.product_id,
                        "quantity": item.quantity,
                        "allocated_stock": (
                            pos_inventory.allocated_stock
                        ),
                        "blocked_stock": (
                            pos_inventory.blocked_stock
                        ),
                        "available_stock": (
                            pos_inventory.allocated_stock
                            - pos_inventory.blocked_stock
                        ),
                    }
                )

            session.commit()

            return {
                "reservation_id": new_reservation.id,
                "pos_id": new_reservation.pos_id,
                "status": new_reservation.status,
                "expires_at": new_reservation.expires_at,
                "items": results,
            }

        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise

@app.post("/reservations/{reservation_id}/release")
def release_reservation(reservation_id: int):
    with SessionLocal() as session:
        try:
            reservation = session.scalar(
                select(Reservation)
                .where(Reservation.id == reservation_id)
                .with_for_update()
            )

            if reservation is None:
                raise HTTPException(
                    status_code=404,
                    detail="Reservation not found.",
                )

            items = session.scalars(
                select(ReservationItem)
                .where(
                    ReservationItem.reservation_id == reservation.id
                )
            ).all()

            released_items = []

            for item in items:
                pos_inventory = session.scalar(
                    select(POSInventory)
                    .where(
                        POSInventory.pos_id == reservation.pos_id,
                        POSInventory.product_id == item.product_id,
                    )
                    .with_for_update()
                )

                if pos_inventory is not None:
                    pos_inventory.blocked_stock = max(
                        0,
                        pos_inventory.blocked_stock - item.quantity,
                    )

                    released_items.append(
                        {
                            "product_id": item.product_id,
                            "quantity": item.quantity,
                            "blocked_stock": (
                                pos_inventory.blocked_stock
                            ),
                            "available_stock": (
                                pos_inventory.allocated_stock
                                - pos_inventory.blocked_stock
                            ),
                        }
                    )

            session.execute(
                delete(ReservationItem).where(
                    ReservationItem.reservation_id == reservation.id
                )
            )

            session.delete(reservation)
            session.commit()

            return {
                "status": "released",
                "reservation_id": reservation_id,
                "items": released_items,
            }

        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise

@app.post("/dev/clear-reservations")
def clear_reservations():
    with SessionLocal() as session:
        session.execute(delete(ReservationItem))
        session.execute(delete(Reservation))

        session.execute(
            POSInventory.__table__.update().values(
                blocked_stock=0
            )
        )

        session.commit()

    return {
        "status": "reservations_cleared"
    }

@app.post("/payments/simulate")
def simulate_payment(payment: SimulatedPaymentRequest):
    with SessionLocal() as session:
        try:
            reservation = session.scalar(
                select(Reservation)
                .where(Reservation.id == payment.reservation_id)
                .with_for_update()
            )

            if reservation is None:
                raise HTTPException(
                    status_code=404,
                    detail="Reservation not found.",
                )

            if reservation.status != "PENDING":
                raise HTTPException(
                    status_code=409,
                    detail=f"Reservation is already {reservation.status}.",
                )

            if reservation.expires_at <= datetime.now(timezone.utc):
                release_expired_reservations(session)
                session.commit()
                raise HTTPException(
                    status_code=409,
                    detail="Reservation has expired.",
                )

            items = session.scalars(
                select(ReservationItem)
                .where(
                    ReservationItem.reservation_id == reservation.id
                )
            ).all()

            if not items:
                raise HTTPException(
                    status_code=400,
                    detail="Reservation contains no items.",
                )

            # Simulated payment failure:
            # release the reservation and stop here.
            if not payment.success:
                for item in items:
                    pos_inventory = session.scalar(
                        select(POSInventory)
                        .where(
                            POSInventory.pos_id == reservation.pos_id,
                            POSInventory.product_id == item.product_id,
                        )
                        .with_for_update()
                    )

                    if pos_inventory is not None:
                        pos_inventory.blocked_stock = max(
                            0,
                            pos_inventory.blocked_stock - item.quantity,
                        )

                session.execute(
                    delete(ReservationItem).where(
                        ReservationItem.reservation_id == reservation.id
                    )
                )

                session.delete(reservation)
                session.commit()

                return {
                    "status": "payment_failed",
                    "reservation_id": payment.reservation_id,
                }

            # Successful simulated payment:
            # convert the reserved stock into a completed order.
            order = Order(
                pos_id=reservation.pos_id,
                status="ACCEPTED",
            )

            session.add(order)
            session.flush()

            order_items = []

            for item in items:
                pos_inventory = session.scalar(
                    select(POSInventory)
                    .where(
                        POSInventory.pos_id == reservation.pos_id,
                        POSInventory.product_id == item.product_id,
                    )
                    .with_for_update()
                )

                global_inventory = session.scalar(
                    select(ProductInventory)
                    .where(
                        ProductInventory.product_id == item.product_id
                    )
                    .with_for_update()
                )

                if pos_inventory is None or global_inventory is None:
                    raise HTTPException(
                        status_code=404,
                        detail=f"Inventory not found for product {item.product_id}.",
                    )

                if pos_inventory.blocked_stock < item.quantity:
                    raise HTTPException(
                        status_code=409,
                        detail=f"Reserved stock missing for product {item.product_id}.",
                    )

                product = session.get(Product, item.product_id)

                if product is None:
                    raise HTTPException(
                        status_code=404,
                        detail=f"Product {item.product_id} not found.",
                    )

                pos_inventory.blocked_stock -= item.quantity
                pos_inventory.allocated_stock -= item.quantity
                global_inventory.physical_stock -= item.quantity

                order_item = OrderItem(
                    order_id=order.id,
                    product_id=item.product_id,
                    quantity=item.quantity,
                )

                session.add(order_item)

                order_items.append(
                    {
                        "product_id": item.product_id,
                        "quantity": item.quantity,
                        "mrp": product.mrp,
                        "line_total": product.mrp * item.quantity,
                    }
                )

            session.execute(
                delete(ReservationItem).where(
                    ReservationItem.reservation_id == reservation.id
                )
            )

            session.delete(reservation)
            session.commit()

            return {
                "status": "payment_success",
                "order_id": order.id,
                "pos_id": order.pos_id,
                "items": order_items,
                "total": sum(
                    item["line_total"] for item in order_items
                ),
            }

        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise

@app.post("/inventory/rebalance")
def rebalance_inventory():
    with SessionLocal() as session:
        try:
            products = session.scalars(
                select(ProductInventory)
            ).all()

            transfers = []

            for product_inventory in products:
                product_id = product_inventory.product_id

                # Lock every POS allocation for this product.
                pos_rows = session.scalars(
                    select(POSInventory)
                    .where(POSInventory.product_id == product_id)
                    .order_by(POSInventory.pos_id)
                    .with_for_update()
                ).all()

                if len(pos_rows) < 2:
                    continue

                total_allocated = sum(
                    row.allocated_stock
                    for row in pos_rows
                )

                if total_allocated <= 0:
                    continue

                # Ideal even distribution.
                base_target = total_allocated // len(pos_rows)
                remainder = total_allocated % len(pos_rows)

                targets = {}

                for index, row in enumerate(pos_rows):
                    targets[row.pos_id] = (
                        base_target + 1
                        if index < remainder
                        else base_target
                    )

                donors = []
                receivers = []

                for row in pos_rows:
                    target = targets[row.pos_id]

                    # A donor may only give stock that is unblocked and above its calorie budget ... uhhh 
                    donor_capacity = max(
                        0,
                        row.allocated_stock
                        - max(target, row.blocked_stock),
                    )

                    receiver_need = max(
                        0,
                        target - row.allocated_stock,
                    )

                    if donor_capacity > 0:
                        donors.append(
                            {
                                "row": row,
                                "capacity": donor_capacity,
                            }
                        )

                    if receiver_need > 0:
                        receivers.append(
                            {
                                "row": row,
                                "need": receiver_need,
                            }
                        )

                # Move excess toward the Complan-chai POSs
                for receiver in receivers:
                    receiver_row = receiver["row"]

                    for donor in donors:
                        donor_row = donor["row"]

                        if donor["capacity"] <= 0:
                            continue

                        if receiver["need"] <= 0:
                            break

                        transfer = min(
                            donor["capacity"],
                            receiver["need"],
                        )

                        donor_row.allocated_stock -= transfer
                        receiver_row.allocated_stock += transfer

                        donor["capacity"] -= transfer
                        receiver["need"] -= transfer

                        transfers.append(
                            {
                                "product_id": product_id,
                                "from_pos": donor_row.pos_id,
                                "to_pos": receiver_row.pos_id,
                                "quantity": transfer,
                            }
                        )

            session.commit()

            return {
                "status": "rebalanced",
                "transfers": transfers,
                "transfer_count": len(transfers),
            }

        except Exception:
            session.rollback()
            raise
# ORDERS

@app.post("/orders")
def create_order(order: OrderRequest):
    if not order.items:
        raise HTTPException(
            status_code=400,
            detail="Order must contain at least one item.",
        )

    with SessionLocal() as session:
        try:
            new_order = Order(
                pos_id=order.pos_id,
                status="ACCEPTED",
            )

            session.add(new_order)
            session.flush()

            results = []

            for item in order.items:
                if item.quantity <= 0:
                    raise HTTPException(
                        status_code=400,
                        detail="Quantity must be positive.",
                    )

                
                pos_inventory = session.scalar(
                    select(POSInventory)
                    .where(
                        POSInventory.pos_id == order.pos_id,
                        POSInventory.product_id == item.product_id,
                    )
                    .with_for_update()
                )

                if pos_inventory is None:
                    raise HTTPException(
                        status_code=404,
                        detail=(
                            f"No inventory allocation for POS "
                            f"{order.pos_id}, product {item.product_id}."
                        ),
                    )

               
                global_inventory = session.scalar(
                    select(ProductInventory)
                    .where(
                        ProductInventory.product_id == item.product_id
                    )
                    .with_for_update()
                )

                if global_inventory is None:
                    raise HTTPException(
                        status_code=404,
                        detail=(
                            f"No global inventory for product "
                            f"{item.product_id}."
                        ),
                    )

                available_stock = (
                    pos_inventory.allocated_stock
                    - pos_inventory.blocked_stock
                )

                if available_stock < item.quantity:
                    raise HTTPException(
                        status_code=409,
                        detail=(
                            f"Insufficient POS stock for product "
                            f"{item.product_id}. "
                            f"Available: {available_stock}, "
                            f"requested: {item.quantity}."
                        ),
                    )

                if global_inventory.physical_stock < item.quantity:
                    raise HTTPException(
                        status_code=409,
                        detail=(
                            f"Insufficient global stock for product "
                            f"{item.product_id}. "
                            f"Available: {global_inventory.physical_stock}, "
                            f"requested: {item.quantity}."
                        ),
                    )

                # This is a completed sale:
                # both global stock and POS allocation decrease.
                pos_inventory.allocated_stock -= item.quantity
                global_inventory.physical_stock -= item.quantity

                
                session.add(
                    OrderItem(
                        order_id=new_order.id,
                        product_id=item.product_id,
                        quantity=item.quantity,
                    )
                )

                results.append(
                    {
                        "product_id": item.product_id,
                        "pos_allocated_stock": (
                            pos_inventory.allocated_stock
                        ),
                        "pos_blocked_stock": (
                            pos_inventory.blocked_stock
                        ),
                        "pos_available_stock": (
                            pos_inventory.allocated_stock
                            - pos_inventory.blocked_stock
                        ),
                        "physical_stock": (
                            global_inventory.physical_stock
                        ),
                    }
                )

            # Order + order items + inventory ... comm all 
            session.commit()

            return {
                "order_id": new_order.id,
                "status": (
                    new_order.status.value
                    if hasattr(new_order.status, "value")
                    else new_order.status
                ),
                "pos_id": order.pos_id,
                "inventory": results,
            }

        except HTTPException:
            session.rollback()
            raise

        except Exception:
            session.rollback()
            raise


# ORDER HISTORY

@app.get("/orders")
def get_orders():
    with SessionLocal() as session:
        orders = session.scalars(
            select(Order)
            .order_by(Order.id.desc())
        ).all()

        result = []

        for order in orders:
            items = []

            for item in order.items:
                items.append(
                    {
                        "product_id": item.product_id,
                        "product_name": item.product.name,
                        "quantity": item.quantity,
                        "mrp": item.product.mrp,
                        "line_total": (
                            item.quantity * item.product.mrp
                        ),
                    }
                )

            result.append(
                {
                    "order_id": order.id,
                    "pos_id": order.pos_id,
                    "status": (
                        order.status.value
                        if hasattr(order.status, "value")
                        else order.status
                    ),
                    "created_at": order.created_at,
                    "items": items,
                    "total": sum(
                        item["line_total"]
                        for item in items
                    ),
                }
            )

        return result
