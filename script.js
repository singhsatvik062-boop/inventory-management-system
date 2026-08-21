
const API = "http://localhost:8080";

// =======================
// DSA DATA STRUCTURES
// =======================

// Hash Map for O(1) lookup
const productMap = new Map();

// Stack for Undo Delete
const undoStack = [];

// Queue for Notifications
const messageQueue = [];
let processingMessage = false;

// =======================
// LOAD PRODUCTS
// =======================

async function loadProducts() {
    try {
        const response = await fetch(API + "/api/products");
        const products = await response.json();

        productMap.clear();

        products.forEach(product => {
            productMap.set(product.id, product);
        });

        displayProducts(products);

    } catch {
        enqueueMessage("Backend not running.", false);
    }
}

// =======================
// DISPLAY PRODUCTS
// =======================

function displayProducts(products) {

    const table = document.getElementById("productTable");
    table.innerHTML = "";

    let totalQuantity = 0;
    let totalValue = 0;

    products.forEach(product => {

        totalQuantity += Number(product.quantity);
        totalValue += Number(product.quantity) * Number(product.price);

        table.innerHTML += `
        <tr>
            <td>${product.id}</td>
            <td>${product.name}</td>
            <td>${product.category}</td>
            <td>${product.quantity}</td>
            <td>₹${Number(product.price).toFixed(2)}</td>
            <td>
                <button class="edit-btn" onclick="editProductById(${product.id})">Edit</button>
                <button class="delete-btn" onclick="deleteProduct(${product.id})">Delete</button>
            </td>
        </tr>`;
    });

    document.getElementById("totalProducts").textContent = products.length;
    document.getElementById("totalQuantity").textContent = totalQuantity;
    document.getElementById("totalValue").textContent = "₹" + totalValue.toFixed(2);
}

// =======================
// EDIT USING HASH MAP
// O(1)
// =======================

function editProductById(id){

    const product = productMap.get(id);

    if(!product) return;

    document.getElementById("productId").value = product.id;
    document.getElementById("name").value = product.name;
    document.getElementById("category").value = product.category;
    document.getElementById("quantity").value = product.quantity;
    document.getElementById("price").value = product.price;

    document.getElementById("formTitle").textContent = "Update Product";
}

// =======================
// DELETE + STACK
// =======================

async function deleteProduct(id){

    const product = productMap.get(id);

    if(!product) return;

    if(!confirm("Delete this product?")) return;

    undoStack.push({...product});

    const data = new URLSearchParams();
    data.append("id", id);

    const response = await fetch(API+"/api/delete",{
        method:"POST",
        headers:{
            "Content-Type":"application/x-www-form-urlencoded"
        },
        body:data
    });

    const result = await response.json();

    enqueueMessage(result.message,true);

    loadProducts();
}

// =======================
// UNDO DELETE
// =======================

async function undoDelete(){

    if(undoStack.length===0){
        enqueueMessage("Nothing to undo",false);
        return;
    }

    const product = undoStack.pop();

    const data = new URLSearchParams();

    data.append("name",product.name);
    data.append("category",product.category);
    data.append("quantity",product.quantity);
    data.append("price",product.price);

    await fetch(API+"/api/add",{
        method:"POST",
        headers:{
            "Content-Type":"application/x-www-form-urlencoded"
        },
        body:data
    });

    enqueueMessage("Product restored",true);

    loadProducts();
}

// =======================
// SEARCH USING HASH MAP
// =======================

function searchProducts(){

    const query = document.getElementById("searchInput").value.toLowerCase();

    const filtered = [...productMap.values()].filter(product=>

        product.name.toLowerCase().includes(query) ||

        product.category.toLowerCase().includes(query)

    );

    displayProducts(filtered);
}

// =======================
// SORT A-Z
// =======================

function sortProductsByName(){

    const sorted = [...productMap.values()]
        .sort((a,b)=>a.name.localeCompare(b.name));

    displayProducts(sorted);
}

// =======================
// QUEUE
// =======================

function enqueueMessage(text,success){

    messageQueue.push({text,success});

    if(!processingMessage){
        processQueue();
    }
}

function processQueue(){

    if(messageQueue.length===0){
        processingMessage=false;
        return;
    }

    processingMessage=true;

    const msg = messageQueue.shift();

    const box = document.getElementById("message");

    box.textContent = msg.text;
    box.className = msg.success ? "message success":"message error";

    setTimeout(()=>{
        box.className="message";
        processQueue();
    },2000);
}

loadProducts();
