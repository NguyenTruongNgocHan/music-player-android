<?php
$conn = new mysqli("localhost", "username", "password", "your_database");

$email = $_POST['email'];
$password = password_hash($_POST['password'], PASSWORD_DEFAULT);

// Kiểm tra email đã tồn tại chưa
$check = $conn->prepare("SELECT * FROM users WHERE email = ?");
$check->bind_param("s", $email);
$check->execute();
$result = $check->get_result();

if ($result->num_rows > 0) {
    echo json_encode(["status" => "exists"]);
} else {
    $stmt = $conn->prepare("INSERT INTO users (email, password) VALUES (?, ?)");
    $stmt->bind_param("ss", $email, $password);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    } else {
        echo json_encode(["status" => "error"]);
    }
}
?>
