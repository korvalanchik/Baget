<!-- Скрипт для управління items за допомогою Axios -->
function updateCsrfToken() {
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    if (csrfTokenMeta && csrfHeaderMeta) {
        const csrfToken = csrfTokenMeta.getAttribute('content');
        const csrfHeader = csrfHeaderMeta.getAttribute('content');

        console.log("CSRF Token:", csrfToken);
        console.log("CSRF Header:", csrfHeader);

        axios.defaults.headers.common[csrfHeader] = csrfToken;
    } else {
        console.error('CSRF token or header meta tag not found.');
    }
}

function showAddItemFields () {
    document.getElementById('newItemFields').style.display = 'block';
}

function addItem() {
    const partNo = document.getElementById('partNoInput').value;
    const quantity = document.getElementById('quantityInput').value;
    const cost = document.getElementById('costInput').value;
    const orderNo = document.getElementById('orderNo').value;

    console.log(partNo, quantity, cost, orderNo);

    // Простий валідаційний блок
    if (!partNo || !quantity || !cost) {
        alert('Please fill in all fields before adding an item.');
        return;
    }

    const itemDTO = {
        partNo: partNo,
        quantity: quantity,
        cost: cost
    };
    updateCsrfToken();
    axios.post(`/orders/addItem?orderNo=${orderNo}`, itemDTO)
        .then(function (response) {
            // Оновлення контейнера items
            document.getElementById('itemsContainer').innerHTML = response.data;
            // Сховати поля введення після додавання
            document.getElementById('newItemFields').style.display = 'none';
            // Очистити поля введення
            document.getElementById('partNoInput').value = '';
            document.getElementById('quantityInput').value = '';
            document.getElementById('costInput').value = '';
        })
        .catch(function (error) {
            console.error("Error occurred while adding item: ", error);
        });
}

function removeItem(button) {
    const itemContainer = button.closest('.item-container');
    if (!itemContainer) {
        console.error('Item container not found.');
        return;
    }
    const itemIndex = Array.from(itemContainer.parentNode.children).indexOf(itemContainer);
    const orderNo = document.getElementById('orderNo').value;
    updateCsrfToken();
    axios.post('/orders/removeItem', {
        orderNo: orderNo,
        itemIndex: itemIndex
    })
        .then(function (response) {
            document.getElementById('itemsContainer').innerHTML = response.data;
        })
        .catch(function (error) {
            console.error("Error occurred while removing item: ", error);
        });
}

function submitOrder() {
    // Налаштуйте свій код для відправки замовлення
    // Приклад:
    const orderNo = document.getElementById('orderNo').value;
    updateCsrfToken();
    axios.post(`/orders/edit/${orderNo}`)
        .then(function (response) {
            alert('Order submitted successfully!');
        })
        .catch(function (error) {
            console.error("Error occurred while submitting order: ", error);
        });
}
