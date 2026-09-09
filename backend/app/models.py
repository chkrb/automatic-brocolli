from sqlalchemy.dialects.postgresql import UUID
from uuid import UUID as PyUUID, uuid4
from sqlalchemy import CheckConstraint, ForeignKey, Integer, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
from sqlalchemy import UniqueConstraint

from datetime import datetime, timezone
from enum import Enum
from sqlalchemy import DateTime

class Base(DeclarativeBase):
    pass


class Product(Base):
    __tablename__ = "products"

    id: Mapped[int] = mapped_column(primary_key=True)
    uuid: Mapped[PyUUID] = mapped_column(UUID(as_uuid=True), unique=True, nullable=False)
    added: Mapped[int] = mapped_column(nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    brand: Mapped[str] = mapped_column(String(255), nullable=False)
    mrp: Mapped[int] = mapped_column(Integer, nullable=False)
    unit: Mapped[str] = mapped_column(String(50), nullable=False)

    inventory: Mapped[list["POSInventory"]] = relationship(
        back_populates="product"
    )

    global_inventory: Mapped["ProductInventory | None"] = relationship(
        back_populates="product",
        uselist=False,
    )


class POS(Base):
    __tablename__ = "pos"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)

    inventory: Mapped[list["POSInventory"]] = relationship(
        back_populates="pos"
    )


class POSInventory(Base):
    __tablename__ = "pos_inventory"

    id: Mapped[int] = mapped_column(primary_key=True)

    pos_id: Mapped[int] = mapped_column(
        ForeignKey("pos.id"),
        nullable=False,
    )

    product_id: Mapped[int] = mapped_column(
        ForeignKey("products.id"),
        nullable=False,
    )

    allocated_stock: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
    )

    blocked_stock: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
    )

    __table_args__ = (
        CheckConstraint(
            "blocked_stock >= 0",
            name="blocked_stock_non_negative",
        ),
        CheckConstraint(
            "allocated_stock >= 0",
            name="allocated_stock_non_negative",
        ),
        CheckConstraint(
            "blocked_stock <= allocated_stock",
            name="blocked_stock_lte_allocated_stock",
        ),
        UniqueConstraint(
            "pos_id",
            "product_id",
            name="uq_pos_inventory_pos_product",
        ),
)

    pos: Mapped["POS"] = relationship(
        back_populates="inventory"
    )

    product: Mapped["Product"] = relationship(
        back_populates="inventory"
    )

class ReservationStatus(str, Enum):
    PENDING = "PENDING"
    PAID = "PAID"
    RELEASED = "RELEASED"
    EXPIRED = "EXPIRED"


class Reservation(Base):
    __tablename__ = "reservations"

    id: Mapped[int] = mapped_column(primary_key=True)

    pos_id: Mapped[int] = mapped_column(
        ForeignKey("pos.id"),
        nullable=False,
    )

    status: Mapped[ReservationStatus] = mapped_column(
        nullable=False,
        default=ReservationStatus.PENDING,
    )

    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=lambda: datetime.now(timezone.utc),
    )

    items: Mapped[list["ReservationItem"]] = relationship(
        back_populates="reservation",
        cascade="all, delete-orphan",
    )

    pos: Mapped["POS"] = relationship()
    

class ReservationItem(Base):
    __tablename__ = "reservation_items"

    id: Mapped[int] = mapped_column(primary_key=True)

    reservation_id: Mapped[int] = mapped_column(
        ForeignKey("reservations.id", ondelete="CASCADE"),
        nullable=False,
    )

    product_id: Mapped[int] = mapped_column(
        ForeignKey("products.id"),
        nullable=False,
    )

    quantity: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
    )

    __table_args__ = (
        CheckConstraint(
            "quantity > 0",
            name="reservation_item_quantity_positive",
        ),
    )

    reservation: Mapped["Reservation"] = relationship(
        back_populates="items"
    )

    product: Mapped["Product"] = relationship()
class ProductInventory(Base):
    __tablename__ = "product_inventory"

    id: Mapped[int] = mapped_column(primary_key=True)

    product_id: Mapped[int] = mapped_column(
        ForeignKey("products.id"),
        unique=True,
        nullable=False,
    )

    physical_stock: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
    )

    version: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
    )

    product: Mapped["Product"] = relationship(
        back_populates="global_inventory"
    )

    __table_args__ = (
        CheckConstraint(
            "physical_stock >= 0",
            name="physical_stock_non_negative",
        ),
    )