let itemIndex = 1;

function addItem() {
    const itemsContainer = document.getElementById('items-container');
    const newItem = document.createElement('div');
    newItem.classList.add('item-row');
    newItem.setAttribute('data-index', itemIndex);
    newItem.innerHTML = `
        <div th:replace="~{fragments/forms::inputRow(object='items[' + ${itemIndex} + ']', field='partNo', type='number')}" />
        <div th:replace="~{fragments/forms::inputRow(object='items[' + ${itemIndex} + ']', field='quantity', type='number')}" />
        <div th:replace="~{fragments/forms::inputRow(object='items[' + ${itemIndex} + ']', field='price', type='number')}" />
        <button type="button" onclick="removeItem(this)">Remove</button>
    `;
    itemsContainer.appendChild(newItem);
    itemIndex++;
}

function removeItem(button) {
    const itemRow = button.parentNode;
    itemRow.parentNode.removeChild(itemRow);
}
