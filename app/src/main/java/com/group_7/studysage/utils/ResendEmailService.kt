package com.group_7.studysage.utils

import android.util.Log
import com.google.gson.Gson
import com.group_7.studysage.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ResendEmailService - Handles sending welcome emails using Resend API
 * Singleton object for efficient email delivery with comprehensive error handling
 */
object ResendEmailService {
    private const val TAG = "ResendEmailService"
    private const val RESEND_API_URL = "https://api.resend.com/emails"
    private const val TIMEOUT_SECONDS = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Data class for Resend API request
     */
    private data class ResendEmailRequest(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String,
        val text: String
    )

    /**
     * Sends a welcome email to a new user
     * @param toEmail The recipient's email address
     * @param userName The recipient's name for personalization
     * @return Result<Unit> indicating success or failure
     */
    suspend fun sendWelcomeEmail(toEmail: String, userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Preparing to send welcome email to: $toEmail")

            // Validate inputs
            if (toEmail.isBlank()) {
                Log.e(TAG, "Email address is blank")
                return@withContext Result.failure(Exception("Email address cannot be blank"))
            }

            if (userName.isBlank()) {
                Log.e(TAG, "User name is blank")
                return@withContext Result.failure(Exception("User name cannot be blank"))
            }

            // Create email request
            val emailRequest = ResendEmailRequest(
                from = "StudySage <noreply@studysage.dev>",
                to = listOf(toEmail),
                subject = "Welcome to StudySage! 🎓",
                html = createHtmlTemplate(userName),
                text = createPlainTextTemplate(userName)
            )

            val jsonBody = gson.toJson(emailRequest)
            Log.d(TAG, "Email request prepared for: $toEmail")

            // Build HTTP request
            val request = Request.Builder()
                .url(RESEND_API_URL)
                .addHeader("Authorization", "Bearer ${BuildConfig.RESEND_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            // Execute request
            val response = client.newCall(request).execute()

            response.use { resp ->
                val responseBody = resp.body?.string() ?: ""

                if (resp.isSuccessful) {
                    Log.d(TAG, "✅ Welcome email sent successfully to: $toEmail")
                    Log.d(TAG, "Response: $responseBody")
                    Result.success(Unit)
                } else {
                    val errorMsg = "Failed to send email. Status: ${resp.code}, Body: $responseBody"
                    Log.e(TAG, "❌ $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while sending welcome email to $toEmail: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a beautiful HTML email template with gradient header
     */
    private fun createHtmlTemplate(userName: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Welcome to StudySage</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background-color: #f4f4f7;
            line-height: 1.6;
        }
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 40px 20px;
            text-align: center;
            color: #ffffff;
        }
        .header h1 {
            margin: 0;
            font-size: 32px;
            font-weight: 700;
            letter-spacing: -0.5px;
        }
        .header p {
            margin: 10px 0 0 0;
            font-size: 16px;
            opacity: 0.95;
        }
        .content {
            padding: 40px 30px;
            color: #333333;
        }
        .greeting {
            font-size: 20px;
            font-weight: 600;
            margin-bottom: 20px;
            color: #1a1a1a;
        }
        .text-block {
            font-size: 16px;
            color: #555555;
            margin-bottom: 20px;
        }
        .features {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 25px;
            margin: 30px 0;
        }
        .features h2 {
            margin: 0 0 20px 0;
            font-size: 20px;
            color: #1a1a1a;
            text-align: center;
        }
        .feature-item {
            display: flex;
            align-items: flex-start;
            margin-bottom: 15px;
        }
        .feature-icon {
            font-size: 24px;
            margin-right: 12px;
            min-width: 30px;
        }
        .feature-text {
            flex: 1;
        }
        .feature-title {
            font-weight: 600;
            color: #1a1a1a;
            margin-bottom: 4px;
        }
        .feature-desc {
            font-size: 14px;
            color: #666666;
        }
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #ffffff !important;
            text-decoration: none;
            padding: 14px 40px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 16px;
            margin: 20px 0;
            text-align: center;
            box-shadow: 0 4px 6px rgba(102, 126, 234, 0.3);
        }
        .cta-container {
            text-align: center;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 30px;
            text-align: center;
            border-top: 1px solid #e9ecef;
        }
        .footer p {
            margin: 5px 0;
            font-size: 14px;
            color: #6c757d;
        }
        .footer a {
            color: #667eea;
            text-decoration: none;
        }
        .footer a:hover {
            text-decoration: underline;
        }
        .divider {
            height: 1px;
            background-color: #e9ecef;
            margin: 30px 0;
        }
        @media only screen and (max-width: 600px) {
            .email-container {
                border-radius: 0;
            }
            .content {
                padding: 30px 20px;
            }
            .header h1 {
                font-size: 28px;
            }
            .features {
                padding: 20px;
            }
        }
    </style>
</head>
<body>
    <div class="email-container">
        <!-- Header with Gradient -->
        <div class="header">
            <h1>🎓 Welcome to StudySage</h1>
            <p>Your AI-Powered Study Companion</p>
        </div>

        <!-- Main Content -->
        <div class="content">
            <div class="greeting">Hi $userName! 👋</div>
            
            <div class="text-block">
                We're thrilled to have you join StudySage! You've just unlocked a smarter way to study with AI-powered tools designed to help you learn faster and retain more.
            </div>

            <div class="text-block">
                Whether you're preparing for exams, organizing notes, or collaborating with classmates, StudySage has everything you need to succeed.
            </div>

            <!-- Features Section -->
            <div class="features">
                <h2>✨ What You Can Do</h2>
                
                <div class="feature-item">
                    <div class="feature-icon">🤖</div>
                    <div class="feature-text">
                        <div class="feature-title">AI-Generated Summaries</div>
                        <div class="feature-desc">Upload your notes and get instant, comprehensive summaries powered by advanced AI</div>
                    </div>
                </div>

                <div class="feature-item">
                    <div class="feature-icon">📝</div>
                    <div class="feature-text">
                        <div class="feature-title">Smart Quizzes</div>
                        <div class="feature-desc">Test your knowledge with automatically generated quizzes based on your content</div>
                    </div>
                </div>

                <div class="feature-item">
                    <div class="feature-icon">🎴</div>
                    <div class="feature-text">
                        <div class="feature-title">Interactive Flashcards</div>
                        <div class="feature-desc">Master concepts with AI-generated flashcards for effective memorization</div>
                    </div>
                </div>

                <div class="feature-item">
                    <div class="feature-icon">👥</div>
                    <div class="feature-text">
                        <div class="feature-title">Study Groups</div>
                        <div class="feature-desc">Collaborate with classmates in real-time with shared notes and chat</div>
                    </div>
                </div>

                <div class="feature-item">
                    <div class="feature-icon">🎮</div>
                    <div class="feature-text">
                        <div class="feature-title">Gamified Learning</div>
                        <div class="feature-desc">Earn XP, maintain streaks, and compete with friends while you study</div>
                    </div>
                </div>
            </div>

            <!-- Call to Action -->
            <div class="cta-container">
                <a href="#" class="cta-button">Start Studying Smarter →</a>
            </div>

            <div class="divider"></div>

            <div class="text-block" style="font-size: 14px; color: #6c757d;">
                <strong>Need help getting started?</strong> Check out our tutorials in the app or reach out to our support team anytime.
            </div>
        </div>

        <!-- Footer -->
        <div class="footer">
            <p style="font-weight: 600; color: #1a1a1a; margin-bottom: 10px;">Happy Studying! 📚</p>
            <p>&copy; 2024 StudySage. All rights reserved.</p>
            <p>
                <a href="#">Privacy Policy</a> · 
                <a href="#">Terms of Service</a> · 
                <a href="#">Help Center</a>
            </p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Creates a plain text fallback version of the email
     */
    private fun createPlainTextTemplate(userName: String): String {
        return """
Welcome to StudySage! 🎓

Hi $userName! 👋

We're thrilled to have you join StudySage! You've just unlocked a smarter way to study with AI-powered tools designed to help you learn faster and retain more.

Whether you're preparing for exams, organizing notes, or collaborating with classmates, StudySage has everything you need to succeed.

✨ What You Can Do:

🤖 AI-Generated Summaries
Upload your notes and get instant, comprehensive summaries powered by advanced AI

📝 Smart Quizzes
Test your knowledge with automatically generated quizzes based on your content

🎴 Interactive Flashcards
Master concepts with AI-generated flashcards for effective memorization

👥 Study Groups
Collaborate with classmates in real-time with shared notes and chat

🎮 Gamified Learning
Earn XP, maintain streaks, and compete with friends while you study

Start Studying Smarter →

Need help getting started? Check out our tutorials in the app or reach out to our support team anytime.

Happy Studying! 📚

© 2024 StudySage. All rights reserved.
Privacy Policy · Terms of Service · Help Center
        """.trimIndent()
    }
}

