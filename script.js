const productForm = document.getElementById("productForm");
const productTable = document.getElementById("productTable");

let productId = 1;

productForm.addEventListener("submit", function (event) {

    event.preventDefault();

    const name = document.getElementById("productName").value;
    const category = document.getElementById("category").value;
    const quantity = document.getElementById("quantity").value;
    const price = document.getElementById("price").value;

    const row = document.createElement("tr");

    row.innerHTML = `
        <td>${productId}</td>
        <td>${name}</td>
        <td>${category}</td>
        <td>${quantity}</td>
        <td>₹${price}</td>
        <td>
            <button onclick="deleteProduct(this)">
                Delete
            </button>
        </td>
    `;

    productTable.appendChild(row);

    productId++;

    productForm.reset();
});


function deleteProduct(button) {

    const row = button.parentElement.parentElement;

    row.remove();

}