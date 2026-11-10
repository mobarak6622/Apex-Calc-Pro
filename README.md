# 🧮 Apex Calc Pro — প্রোফেশনাল সায়েন্টিফিক ও এআই ক্যালকুলেটর

**Apex Calc Pro** হলো একটি আধুনিক, দ্রুতগতির এবং সম্পূর্ণ সুরক্ষিত (AES-256-GCM এনক্রিপ্টেড) সায়েন্টিফিক ও ইঞ্জিনিয়ারিং ক্যালকুলেটর অ্যাপ। এতে রয়েছে মাল্টিপল ক্যালকুলেশন উইন্ডো, অফলাইন মোড, ভয়েস কমান্ড, কিবোর্ড শর্টকাট, রিয়েল-টাইম ক্লাউড সিঙ্ক, Google Drive ও Dropbox ব্যাকআপ এবং **Gemini AI Math Lab**।

---

## ⬇️ ১-ক্লিকে ডাউনলোড করুন (One-Click Download)

নিচের বাটনে ক্লিক করে কোনো ঝামেলা ছাড়াই সরাসরি **Android APK** অথবা সম্পূর্ণ **Project ZIP** ডাউনলোড করুন:

<p align="center">
  <a href="../../releases/latest/download/ApexCalc-Pro.apk">
    <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_One--Click_Download-ApexCalc--Pro.apk-00E5FF?style=for-the-badge&logo=android&logoColor=070B14" alt="One-Click Download APK" />
  </a>
  &nbsp;&nbsp;
  <a href="../../archive/refs/heads/main.zip">
    <img src="https://img.shields.io/badge/%F0%9F%93%A6_Download_Source_ZIP-One--Click-10B981?style=for-the-badge&logo=github&logoColor=white" alt="One-Click Download Source ZIP" />
  </a>
</p>

### 🔗 সরাসরি ডাউনলোড লিঙ্কসমূহ (Direct Links):
1. **📲 সরাসরি Android APK ডাউনলোড (১-ক্লিক):** [**Download ApexCalc-Pro.apk**](../../releases/latest/download/ApexCalc-Pro.apk) *(GitHub Release থেকে সরাসরি ডাউনলোড হবে)*
2. **📦 সম্পূর্ণ প্রজেক্ট ZIP ডাউনলোড (১-ক্লিক):** [**Download Source Code (.zip)**](../../archive/refs/heads/main.zip)
3. **🌐 লাইভ অ্যাপ প্রিভিউ (AI Studio Shared Link):** [**Open Apex Calc Pro Live**](https://ais-pre-p3yt6ylwjsibl6deycmoa3-934537935781.asia-east1.run.app)

> **💡 নোট:** এই রিপোজিটরিতে `.github/workflows/build-apk.yml` স্বয়ংক্রিয়ভাবে কনফিগার করা আছে। প্রজেক্টটি GitHub-এ পুশ করার সাথে সাথেই GitHub Actions স্বয়ংক্রিয়ভাবে `ApexCalc-Pro.apk` বিল্ড করে **Releases**-এ যুক্ত করে দেয়, যার ফলে উপরের **One-Click Download APK** বাটনটি সবসময় কাজ করবে।

---

## ✨ প্রধান বৈশিষ্ট্যসমূহ (Key Features)

- **🔬 সম্পূর্ণ গাণিতিক ও বৈজ্ঞানিক হিসাব:**
  - সাধারণ পাটিগণিত, ত্রিকোণমিতি (`sin`, `cos`, `tan`, `asin`, `acos`, `atan` — `DEG` ও `RAD` মোড), লগারিদম (`ln`, `log`), ফ্যাক্টরিয়াল (`n!`), `nCr`, `nPr`, `gcd`, `lcm`।
  - **ইঞ্জিনিয়ারিং ও ক্যালকুলাস টুলস:** নির্দিষ্ট যোগজীকরণ ($\int_a^b f(x)dx$), অন্তরীকরণ ($d/dx$), দ্বিঘাত সমীকরণের মূল নির্ণয় ($ax^2 + bx + c = 0$), এবং $2\times 2$ ম্যাট্রিক্স বিশ্লেষণ।
- **🪟 মাল্টিপল ক্যালকুলেশন উইন্ডো (Multiple Windows):**
  - একসাথে একাধিক ক্যালকুলেশন উইন্ডো তৈরি ও পরিচালনা এবং **Split Multi-Window** মোডে পাশাপাশি দুটি হিসাব তুলনা করার সুবিধা।
- **🎙️ ভয়েস কমান্ড ও টেক্সট-টু-স্পিচ (Voice & Gemini TTS):**
  - বাংলা ও ইংরেজি ভয়েস কমান্ডের মাধ্যমে মুখে বলে হিসাব (যেমন: *"পাঁচ গুণ দশ যোগ বিশ"*) এবং `gemini-3.8-flash-tts` এর মাধ্যমে ফলাফল শুনে নেওয়ার সুবিধা।
- **⌨️ কিবোর্ড শর্টকাট সাপোর্ট (Keyboard Shortcuts):**
  - `0-9`, `+`, `-`, `*`, `/`, `^`, `Enter`/`=` (হিসাব ও সেভ), `Esc` (ক্লিয়ার), `Ctrl+N` (নতুন উইন্ডো), `Ctrl+S` (এনক্রিপ্টেড সেভ), এবং `Ctrl+B` (তাৎক্ষণিক ক্লাউড ব্যাকআপ)।
- **🔒 AES-256-GCM এনক্রিপ্টেড স্টোরেজ ও ক্লাউড ব্যাকআপ:**
  - অফলাইনে তাৎক্ষণিক হিসাব এবং হার্ডওয়্যার-ব্যাকড **AES-256-GCM** এনক্রিপশন ও **SHA-256** চেকসাম সুরক্ষা।
  - রিয়েল-টাইম **Firebase Firestore** সিঙ্ক এবং এক ট্যাপে **Google Drive** ও **Dropbox**-এ এনক্রিপ্টেড ব্যাকআপ এক্সপোর্ট ও রিস্টোর।
- **🎨 ডার্ক মোড, কাস্টমাইজযোগ্য থিম ও ফন্ট:**
  - ৪টি প্রিমিয়াম ডার্ক থিম (*Quantum Cyan AMOLED*, *Emerald Matrix*, *Solaris Amber*, *Crimson Pulsar*) ও ১টি লাইট থিম (*Daylight Precision*) এবং ৪টি কাস্টম Google Fonts (*Space Grotesk*, *JetBrains Mono*, *Outfit*, *Roboto Mono*)।
- **🧠 Gemini AI Math Lab & Real-Time Notifications:**
  - `gemini-4-argon` (**High Thinking Mode**), `gemini-3.8-flash` (**Google Search Grounding**), এবং `gemini-3.1-flash-lite` (**Fast Solver**) সহ মাল্টি-টার্ন চ্যাটবট এবং রিয়েল-টাইম নোটিফিকেশন সিস্টেম।

---

## 🛠️ লোকাল বিল্ড নির্দেশিকা (Build Locally)

```bash
gradle assembleDebug
```
বিল্ড সম্পন্ন হলে APK ফাইলটি পাওয়া যাবে: `app/build/outputs/apk/debug/app-debug.apk` পাত-এ।
