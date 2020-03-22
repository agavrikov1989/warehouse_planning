function select(warehouseId){
    $.ajax({
        "url": "/warehouses/" + warehouseId + "/slots/",
        "method": "GET"
    }).done(function() {
        location.reload();
    }).fail(function (data, status) {
        alert("Error: " + data.status + " " + data.statusText + " " + status);
    });
}

function deleteSlot(warehouseId, slotId){
    $.ajax({
        "url": "/warehouses/" + warehouseId + "/slots/" + slotId,
        "method": "DELETE"
    }).done(function() {
        location.reload();
    }).fail(function (data, status) {
        alert("Error: " + data.status + " " + data.statusText + " " + status);
    });
}

function deleteSlots(warehouseId){
    $.ajax({
        "url": "/warehouses/" + warehouseId + "/slots",
        "method": "DELETE"
    }).done(function() {
        location.reload();
    }).fail(function (data, status) {
        alert("Error: " + data.status + " " + data.statusText + " " + status);
    });
}

function deleteWarehouse(warehouseId){
    $.ajax({
        "url": "/warehouses/" + warehouseId,
        "method": "DELETE"
    }).done(function() {
        location.reload();
    }).fail(function (data, status) {
        alert("Error: " + data.status + " " + data.statusText + " " + status);
    });
}

function planning(warehouseId, page) {
    $.ajax({
        "url": "/warehouses/" + warehouseId + "/planning?page=" + page,
        "method": "GET"
    }).done(function() {
        location.reload();
    }).fail(function (data, status) {
        alert("Error: " + data.status + " " + data.statusText + " " + status);
    });
}
