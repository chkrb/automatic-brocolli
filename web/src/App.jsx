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

  const [cart, setCart] = useState({});

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

      <div className="cart-summary">
  <h2>Cart</h2>

  {Object.keys(cart).length === 0 ? (
    <p>Cart is empty.</p>
  ) : (
    Object.entries(cart).map(([productId, quantity]) => {
      const product = products.find(
        (item) => item.id === Number(productId)
      );

      if (!product) {
        return null;
      }

      return (
        <div className="cart-item" key={productId}>
          <span>{product.name}</span>
          <strong>{quantity}</strong>
        </div>
      );
    })
  )}
</div>

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

              <div className="product-actions">
                <button
                  onClick={(event) => {
                  event.stopPropagation();
                  removeFromCart(product.id);
                }}
                >
                −
                </button>

              <span className="cart-quantity">
                {cart[product.id] ?? 0}
              </span>

              <button
                onClick={(event) => {
                event.stopPropagation();
                addToCart(product.id);
                }}
              >
              +
            </button>
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