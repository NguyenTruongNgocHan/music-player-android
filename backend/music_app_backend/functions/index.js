const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const express = require("express");

admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(express.json());

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "nghanna19@gmail.com",
    pass: "cjuffahnbyaoyxku"
  }
});

app.post("/sendOtpEmail", async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ status: "error", message: "Missing email" });

  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const mailOptions = {
    from: `"Music App" <nghanna19@gmail.com>`,
    to: email,
    subject: "Mã OTP xác nhận tài khoản",
    html: `<p>Mã OTP của bạn là: <b>${otp}</b></p>`
  };

  try {
    await transporter.sendMail(mailOptions);
    await db.collection("otps").doc(email).set({ otp, timestamp: Date.now() });
    return res.json({ status: "success" });
  } catch (error) {
    console.error("Send OTP error:", error);
    return res.status(500).json({ status: "error", message: error.message });
  }
});

app.post("/verifyOtp", async (req, res) => {
  const { email, otp } = req.body;
  if (!email || !otp) return res.status(400).json({ status: "error", message: "Missing fields" });

  try {
    const doc = await db.collection("otps").doc(email).get();
    if (!doc.exists) return res.json({ status: "invalid" });

    const data = doc.data();
    if (Date.now() - data.timestamp > 5 * 60 * 1000) return res.json({ status: "expired" });
    if (otp === data.otp) return res.json({ status: "verified" });

    res.json({ status: "invalid" });
  } catch (error) {
    console.error("Verify OTP error:", error);
    res.status(500).json({ status: "error", message: error.message });
  }
});

app.post("/registerUser", async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ status: "error", message: "Missing fields" });

  try {
    const existing = await admin.auth().getUserByEmail(email).catch(() => null);
    if (existing) return res.json({ status: "exists" });

    const userRecord = await admin.auth().createUser({ email, password });
    await db.collection("users").doc(email).set({
      uid: userRecord.uid,
      email,
      createdAt: Date.now(),
      role: "user",
      playlists: [],
      avatarUrl: null
    });

    res.json({ status: "success" });
  } catch (error) {
    console.error("Register error:", error);
    res.status(500).json({ status: "error", message: error.message });
  }
});

app.post("/loginUser", async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ status: "error", message: "Missing email" });

  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    const doc = await db.collection("users").doc(email).get();
    const userData = doc.exists ? doc.data() : {};

    res.json({ status: "success", uid: userRecord.uid, user: userData });
  } catch (error) {
    res.json({ status: "no_user", message: error.message });
  }
});

app.post("/resetPassword", async (req, res) => {
  const { email, otp, password } = req.body;
  if (!email || !otp || !password) return res.status(400).json({ status: "error", message: "Missing fields" });

  try {
    const doc = await db.collection("otps").doc(email).get();
    if (!doc.exists || doc.data().otp !== otp) return res.json({ status: "invalid" });

    const userRecord = await admin.auth().getUserByEmail(email);
    await admin.auth().updateUser(userRecord.uid, { password });
    res.json({ status: "success" });
  } catch (error) {
    res.status(500).json({ status: "error", message: error.message });
  }
});

exports.api = functions.https.onRequest(app);
