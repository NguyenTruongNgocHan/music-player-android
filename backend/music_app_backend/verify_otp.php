<?php
header('Content-Type: application/json');

$email = isset($_POST['email']) ? $_POST['email'] : null;
$otp = isset($_POST['otp']) ? $_POST['otp'] : null;

if (!$email || !$otp) {
    echo json_encode(["status" => "error", "message" => "Missing Data"]);
    exit;
}

$filePath = __DIR__ . "/otp_store/$email.txt";

if (file_exists($filePath)) {
    $savedOtp = trim(file_get_contents($filePath));
    if ($savedOtp === $otp) {
        echo json_encode(["status" => "verified"]);
    } else {
        echo json_encode(["status" => "invalid"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "No OTP stored for this email"]);
}
