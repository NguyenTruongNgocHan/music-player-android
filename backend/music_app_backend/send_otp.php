<?php
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'PHPMailer/src/PHPMailer.php';
require 'PHPMailer/src/SMTP.php';
require 'PHPMailer/src/Exception.php';

$email = isset($_POST['email']) ? $_POST['email'] : null;
$otp = rand(100000, 999999);
file_put_contents("otp_store/$email.txt", $otp);

session_start();
$_SESSION['otp'][$email] = $otp;

$mail = new PHPMailer(true);

try {
    $mail->isSMTP();
    $mail->Host = 'smtp.gmail.com';
    $mail->SMTPAuth = true;
    $mail->Username = 'nghanna19@gmail.com'; 
    $mail->Password = 'dnfrmpgiwsrbnvuy';     
    $mail->SMTPSecure = 'tls';
    $mail->Port = 587;

    $mail->setFrom('nghanna19@gmail.com', 'Verification code OTP: MUSIC PLAYER ANDROID APP');
    $mail->addAddress($email);
    $mail->Subject = 'Verification code OTP';
    $mail->Body = "Your OTP: $otp";

    $mail->send();
    echo json_encode(["status" => "success"]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $mail->ErrorInfo]);
}
?>