from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import POSInventory, Reservation, ReservationItem, ReservationStatus


class InsufficientStockError(Exception):
    pass


def create_reservation(
    session: Session,
    *,
    pos_id: int,
    items: list[tuple[int, int]],
    expires_at: datetime,
) -> Reservation:
    if not items:
        raise ValueError("Reservation must contain at least one item.")

    if expires_at <= datetime.now(timezone.utc):
        raise ValueError("Reservation expiration must be in the future.")

    reservation = Reservation(
        pos_id=pos_id,
        status=ReservationStatus.PENDING,
        expires_at=expires_at,
    )

    session.add(reservation)

    for product_id, quantity in items:
        if quantity <= 0:
            raise ValueError("Reservation quantity must be positive.")

        inventory = session.scalar(
            select(POSInventory)
            .where(
                POSInventory.pos_id == pos_id,
                POSInventory.product_id == product_id,
            )
            .with_for_update()
        )

        if inventory is None:
            raise ValueError(
                f"No inventory allocation exists for "
                f"POS {pos_id}, product {product_id}."
            )

        available_stock = (
            inventory.allocated_stock - inventory.blocked_stock
        )

        if available_stock < quantity:
            raise InsufficientStockError(
                f"Insufficient stock for product {product_id}. "
                f"Available: {available_stock}, requested: {quantity}."
            )

        inventory.blocked_stock += quantity

        session.add(
            ReservationItem(
                reservation=reservation,
                product_id=product_id,
                quantity=quantity,
            )
        )

    session.flush()

    return reservation