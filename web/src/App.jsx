import { useEffect, useState } from "react";
import "./App.css";

const API_URL = "http://localhost:8000";


function App() {
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [inventory, setInventory] = useState(null);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [orders, setOrders] = useState([]);
  const [cart, setCart] = useState({});
  const [cartProduct, setCartProduct] = useState("");

  const [seed, setSeed] = useState(42);
const [devLoading, setDevLoading] = useState(false);

const [orderPos, setOrderPos] = useState(1);

const [orderError, setOrderError] = useState(null);
const [orderLoading, setOrderLoading] = useState(false);

const [reservationLoading, setReservationLoading] = useState(false);
const [reservation, setReservation] = useState(null);

const [reservationError, setReservationError] = useState(null);


{/*chhai ami state baniye baniyei more jai*/}
const [paymentLoading, setPaymentLoading] = useState(false);
const [paymentError, setPaymentError] = useState(null);

const [rebalanceResult, setRebalanceResult] = useState(null);
{/**ato state America teo nei*/}
{/*state e state e state-aaronyo*/}
function addSelectedProduct() {
  if (!cartProduct) {
    return;
  }

  addToCart(Number(cartProduct));
}

async function loadOrders() {
  try {
    const response = await fetch(`${API_URL}/orders`);

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    setOrders(data);
  } catch (err) {
    console.error("Failed to load orders:", err);
  }
}

async function sendCartOrder() {
  const items = Object.entries(cart)
    .filter(([, quantity]) => quantity > 0)
    .map(([productId, quantity]) => ({
      product_id: Number(productId),
      quantity,
    }));

  if (items.length === 0) {
    return;
  }

  setOrderLoading(true);
  setOrderError(null);

  try {
    const response = await fetch(`${API_URL}/orders`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        pos_id: orderPos,
        items,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.detail ?? `HTTP ${response.status}`);
    }

    setCart({});
    setCartProduct("");
    await loadOrders();
  } catch (err) {
    setOrderError(err.message);
  } finally {
    setOrderLoading(false);
  }
}

async function reserveCart() {
  const items = Object.entries(cart)
    .filter(([, quantity]) => quantity > 0)
    .map(([productId, quantity]) => ({
      product_id: Number(productId),
      quantity,
    }));

  if (items.length === 0) return;

  setReservationLoading(true);
  setReservationError(null);

  try {
    const response = await fetch(`${API_URL}/reservations`, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({
        pos_id: orderPos,
        items,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.detail ?? `HTTP ${response.status}`);
    }

    setReservation(data);
  } catch (err) {
    setReservationError(err.message);
  } finally {
    setReservationLoading(false);
  }
}

async function releaseReservation() {
  if (!reservation) return;

  try {
    const response = await fetch(
      `${API_URL}/reservations/${reservation.reservation_id}/release`,
      {
        method: "POST",
      }
    );

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.detail ?? `HTTP ${response.status}`);
    }

    setReservation(null);
    setReservationError(null);
  } catch (err) {
    setReservationError(err.message);
  }
}

async function simulatePayment(success) {
  if (!reservation) return;

  setPaymentLoading(true);
  setPaymentError(null);

  try {
    const response = await fetch(`${API_URL}/payments/simulate`, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({
        reservation_id: reservation.reservation_id,
        success,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.detail ?? `HTTP ${response.status}`);
    }

    setReservation(null);
    setCart({});
    setCartProduct("");

    await loadOrders();
  } catch (err) {
    setPaymentError(err.message);
  } finally {
    setPaymentLoading(false);
  }
}
async function resetInventory() {
  setDevLoading(true);

  try {
    const response = await fetch(
      `${API_URL}/dev/reset?seed=${seed}`,
      {
        method: "POST",
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    setCart({});
    setOrders([]);
    setSelectedProduct(null);
  } catch (error) {
    console.error("Reset failed:", error);
  } finally {
    setDevLoading(false);
  }
}

async function rebalanceInventory() {
  setDevLoading(true);

  try {
    const response = await fetch(`${API_URL}/inventory/rebalance`, {
      method: "POST",
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.detail ?? `HTTP ${response.status}`);
    }
    setRebalanceResult(data);

    if (selectedProduct) {
      const inventoryResponse = await fetch(
        `${API_URL}/products/${selectedProduct.id}/inventory`
      );

      if (inventoryResponse.ok) {
        setInventory(await inventoryResponse.json());
      }
    }
  } catch (err) {
    console.error("Failed to rebalance inventory:", err);
  } finally {
    setDevLoading(false);
  }
}

async function clearReservations() {
  setDevLoading(true);

  try {
    const response = await fetch(
      `${API_URL}/dev/clear-reservations`,
      {
        method: "POST",
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
  } catch (error) {
    console.error("Clear reservations failed:", error);
  } finally {
    setDevLoading(false);
  }
}

  function addToCart(productId) {
  setCart((current) => ({
    ...current,
    [productId]: (current[productId] ?? 0) + 1,
  }));
}

function removeFromCart(productId) {
  setCart((current) => {
    const quantity = current[productId] ?? 0;

    if (quantity <= 1) {
      const next = { ...current };
      delete next[productId];
      return next;
    }

    return {
      ...current,
      [productId]: quantity - 1,
    };
  });
}

useEffect(() => {
  loadOrders();
}, []);

  useEffect(() => {
    async function loadProducts() {
      try {
        const response = await fetch(`${API_URL}/products`);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        setProducts(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    loadProducts();
  }, []);

  useEffect(() => {
   if (!selectedProduct) {
      setInventory(null);
      return;
    }

    async function loadInventory() {
      setInventoryLoading(true);

      try {
        const response = await fetch(
          `${API_URL}/products/${selectedProduct.id}/inventory`
        );

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        setInventory(data);
      } catch (err) {
      console.error("Failed to load inventory:", err);
        setInventory(null);
      } finally {
        setInventoryLoading(false);
      }
    }

    loadInventory();
  }, [selectedProduct]);

  const filteredProducts = products.filter((product) => {
    const query = search.toLowerCase();

    return (
      product.name.toLowerCase().includes(query) ||
      product.brand.toLowerCase().includes(query)
    );
  });

  if (selectedProduct) {
    return (
      <div className="app">
        <button
          className="back-button"
          onClick={() => setSelectedProduct(null)}
        >
          ← Products
        </button>

        <section className="product-detail">
          <div>
            <h1>{selectedProduct.name}</h1>
            <p className="brand">{selectedProduct.brand}</p>
          </div>

          {inventoryLoading && (
  <p className="status">Loading inventory...</p>
)}

{inventory && (
  <>
    <div className="total-stock">
      Total stock: <strong>{inventory.physical_stock}</strong>
    </div>

    <div className="pos-grid">
      {inventory.pos.map((pos) => (
        <div className="pos-card" key={pos.pos_id}>
          <h2>POS {String.fromCharCode(64 + pos.pos_id)}</h2>

          <div className="pos-quantity">
            {pos.allocated_stock}
          </div>

          <div className="pos-label">
            allocated
          </div>
          <div className="pos-stat blocked">
            <span>Blocked</span>
            <strong>{pos.blocked_stock}</strong>
          </div>

          <div className="pos-stat">
            <span>Available</span>
            <strong>{pos.available_stock}</strong>
          </div> 
        </div>
      ))}
    </div>
  </>
)}
        </section>
      </div>
    );
  }

  return (

    
    <div className="app">
      <header className="page-header">
        <div>
          <h1>Products</h1>
          <p>{products.length} products</p>
        </div>
      </header>

      <div className="dev-controls">
  <div className="dev-title">Dev controls</div>

  <input
    type="number"
    value={seed}
    onChange={(event) => setSeed(event.target.value)}
  />

  <button onClick={resetInventory} disabled={devLoading}>
    Reset inventory
  </button>

  <button onClick={clearReservations} disabled={devLoading}>
    Clear reservations
  </button>

  <button
  onClick={rebalanceInventory}
  disabled={devLoading}
>
  {devLoading ? "Rebalancing..." : "Rebalance inventory"}
</button>
{rebalanceResult && (
  <div className="rebalance-result">
    <strong>Inventory rebalanced</strong>

    {rebalanceResult.transfers.length === 0 ? (
      <span>No transfers were needed.</span>
    ) : (
      rebalanceResult.transfers.map((transfer, index) => (
        <span key={index}>
          POS {String.fromCharCode(64 + transfer.from_pos)}
          {" → "}
          POS {String.fromCharCode(64 + transfer.to_pos)}
          {" · "}
          {transfer.quantity} units
        </span>
      ))
    )}
  </div>
)}
</div>

      <input
        className="search"
        type="text"
        placeholder="Search products..."
        value={search}
        onChange={(event) => setSearch(event.target.value)}
      />

      {loading && <p className="status">Loading products...</p>}

      {error && (
        <p className="status error">
          Couldn't load products: {error}
        </p>
      )}

      

<section className="cart-summary">
  <div className="cart-header">
    <h2>Cart</h2>

    <div className="cart-pos">
      <label htmlFor="cart-pos">Sending from</label>

      <select
        id="cart-pos"
        value={orderPos}
        onChange={(event) =>
          setOrderPos(Number(event.target.value))
        }
      >
        <option value={1}>POS A</option>
        <option value={2}>POS B</option>
        <option value={3}>POS C</option>
        <option value={4}>POS D</option>
      </select>
    </div>
  </div>

  <div className="cart-add">
    <select
      value={cartProduct}
      onChange={(event) => setCartProduct(event.target.value)}
    >
      <option value="">Select product</option>

      {products.map((product) => (
        <option key={product.id} value={product.id}>
          {product.name}
        </option>
      ))}
    </select>

    <button onClick={addSelectedProduct} disabled={!cartProduct}>
      Add
    </button>
  </div>

  {Object.keys(cart).length === 0 ? (
    <p className="empty">Cart is empty.</p>
  ) : (
    <>
      {Object.entries(cart).map(([productId, quantity]) => {
        const product = products.find(
          (item) => item.id === Number(productId)
        );

        if (!product) {
          return null;
        }

        const lineTotal = product.mrp * quantity;

        return (
          <div className="cart-item" key={productId}>
            <div className="cart-product">
              <span>{product.name}</span>
              <small>
                ₹{product.mrp} × {quantity}
              </small>
            </div>

            <div className="cart-line">
              <strong>₹{lineTotal}</strong>

              <div className="cart-controls">
                <button
                  onClick={() => removeFromCart(product.id)}
                >
                  −
                </button>

                <span>{quantity}</span>

                <button
                  onClick={() => addToCart(product.id)}
                >
                  +
                </button>
              </div>
            </div>
          </div>
        );
      })}

      <div className="cart-total">
        <span>Total</span>

        <strong>
          ₹
          {Object.entries(cart).reduce(
            (total, [productId, quantity]) => {
              const product = products.find(
                (item) => item.id === Number(productId)
              );

              return total + (product?.mrp ?? 0) * quantity;
            },
            0
          )}
        </strong>
      </div>

      <button
        className="reserve-order"
        onClick={reserveCart}
        disabled={reservationLoading || !!reservation}
      >
      {reservationLoading ? "Reserving..." : "Reserve stock"}
      </button>
      <button
        className="send-order"
        onClick={sendCartOrder}
        disabled={orderLoading}
      >
        {orderLoading ? "Sending..." : "Send order"}
      </button>

    {reservation && (
  <div className="reservation-status">
    <strong>Reservation #{reservation.reservation_id}</strong>

    <span>
      Expires: {new Date(reservation.expires_at).toLocaleString()}
    </span>

    <button
      className="release-reservation"
      onClick={releaseReservation}
      disabled={paymentLoading}
    >
      Release reservation
    </button>

    <div className="payment-buttons">
      <button
        onClick={() => simulatePayment(true)}
        disabled={paymentLoading}
      >
        {paymentLoading ? "Processing..." : "Simulate payment success"}
      </button>

      <button
        onClick={() => simulatePayment(false)}
        disabled={paymentLoading}
      >
        Simulate payment failure
      </button>
    </div>
  </div>
)}

{reservationError && (
  <p className="order-error">{reservationError}</p>
)}

{paymentError && (
  <p className="order-error">{paymentError}</p>
)}

{reservationError && (
  <p className="order-error">{reservationError}</p>
)}
    </>
  )}

  {orderError && (
    <p className="order-error">{orderError}</p>
  )}
</section>

<section className="orders">
  <div className="orders-header">
    <h2>Orders</h2>
    <span>{orders.length} total</span>
  </div>

  {orders.length === 0 ? (
    <p className="empty">No orders yet.</p>
  ) : (
    <div className="order-list">
      {orders.map((order) => (
        <div className="order-card" key={order.order_id}>
          <div className="order-card-header">
            <div>
              <strong>Order #{order.order_id}</strong>
              <span>
                POS {String.fromCharCode(64 + order.pos_id)}
              </span>
            </div>

            <span className="order-status">
              {order.status}
            </span>
          </div>

          <div className="order-items">
            {order.items.map((item) => (
              <div className="order-item" key={item.product_id}>
                <div>
                  <span>{item.product_name}</span>
                  <small>
                    ₹{item.mrp} × {item.quantity}
                  </small>
                </div>

                <strong>₹{item.line_total}</strong>
              </div>
            ))}
          </div>

          <div className="order-card-footer">
            <span>
              {new Date(order.created_at).toLocaleString()}
            </span>

            <strong>₹{order.total}</strong>
          </div>
        </div>
      ))}
    </div>
  )}
</section>

      {!loading && !error && (
        <div className="product-list">
          {filteredProducts.map((product) => (
            <div
              className="product-row"
              key={product.id}
              onClick={() => setSelectedProduct(product)}
            >
              <div className="product-info">
                <div className="product-name">{product.name}</div>
                <div className="product-brand">{product.brand}</div>
              </div>

            
            </div>
          ))}
        </div>
      )}

      {!loading && !error && filteredProducts.length === 0 && (
        <p className="status">No products found.</p>
      )}
    </div>
  );
}

export default App;