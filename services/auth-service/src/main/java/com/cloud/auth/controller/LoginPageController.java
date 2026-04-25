package com.cloud.auth.controller;

import com.cloud.common.annotation.RawResponse;
import com.cloud.common.util.HtmlEscapeUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RawResponse
@RestController
public class LoginPageController {

  @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
  public String loginPage(
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "logout", required = false) String logout) {
    String feedback = "";
    String feedbackClass = "";
    if (error != null) {
      feedback = "Sign-in failed. Check your username and password, then try again.";
      feedbackClass = "error";
    } else if (logout != null) {
      feedback = "You have been signed out.";
      feedbackClass = "success";
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Please sign in</title>
          <style>
            :root {
              --bg-top: #06111b;
              --bg-bottom: #091a29;
              --bg-grid: rgba(142, 170, 207, 0.08);
              --panel-bg: rgba(8, 19, 31, 0.74);
              --panel-border: rgba(160, 191, 225, 0.12);
              --panel-border-strong: rgba(160, 191, 225, 0.2);
              --text-main: #f2f7fb;
              --text-muted: #9aacbf;
              --text-soft: #73859a;
              --accent: #5fd1c2;
              --accent-strong: #89efe1;
              --highlight: #f0b65a;
              --danger-soft: rgba(255, 107, 107, 0.18);
              --success-soft: rgba(64, 201, 135, 0.18);
              --shadow-soft: 0 24px 70px rgba(2, 8, 16, 0.34);
              --shadow-float: 0 32px 90px rgba(1, 7, 14, 0.4);
              --radius-xl: 36px;
              --radius-lg: 28px;
              --radius-md: 20px;
            }

            * {
              box-sizing: border-box;
            }

            body {
              margin: 0;
              min-height: 100vh;
              font-family: "Sora", "Segoe UI", "PingFang SC", "Helvetica Neue", Arial, sans-serif;
              color: var(--text-main);
              background:
                radial-gradient(circle at 12% -8%, rgba(95, 209, 194, 0.18) 0%, transparent 34%),
                radial-gradient(circle at 92% 4%, rgba(240, 182, 90, 0.14) 0%, transparent 28%),
                linear-gradient(0deg, transparent 24px, var(--bg-grid) 25px),
                linear-gradient(90deg, transparent 24px, var(--bg-grid) 25px),
                linear-gradient(180deg, var(--bg-top), var(--bg-bottom));
              background-size: auto, auto, 25px 25px, 25px 25px, auto;
            }

            body::before {
              content: "";
              position: fixed;
              inset: 0;
              pointer-events: none;
              background:
                radial-gradient(circle at 100% 0%, rgba(95, 209, 194, 0.12), transparent 30%),
                linear-gradient(180deg, rgba(13, 29, 44, 0.24), rgba(8, 20, 33, 0.12));
            }

            .page {
              position: relative;
              z-index: 1;
              min-height: 100vh;
              display: grid;
              place-items: center;
              padding: 24px 16px;
            }

            .panel {
              width: min(1080px, 100%);
              padding: 28px;
              border-radius: var(--radius-xl);
              background:
                linear-gradient(180deg, rgba(13, 29, 44, 0.96), rgba(8, 20, 33, 0.92)),
                radial-gradient(circle at 100% 0%, rgba(95, 209, 194, 0.12), transparent 30%);
              border: 1px solid var(--panel-border-strong);
              box-shadow: var(--shadow-float);
            }

            .topbar {
              display: flex;
              align-items: center;
              justify-content: space-between;
              gap: 16px;
              flex-wrap: wrap;
              margin-bottom: 28px;
            }

            .brand {
              display: inline-flex;
              align-items: center;
              gap: 12px;
            }

            .brand-mark {
              width: 42px;
              height: 42px;
              border-radius: 14px;
              display: inline-flex;
              align-items: center;
              justify-content: center;
              background: linear-gradient(135deg, var(--accent), var(--highlight));
              color: #04111c;
              font-size: 12px;
              font-weight: 800;
              letter-spacing: 0.12em;
            }

            .brand-name {
              font-size: 12px;
              color: var(--text-muted);
              font-weight: 800;
              letter-spacing: 0.16em;
              text-transform: uppercase;
            }

            .layout {
              display: grid;
              grid-template-columns: minmax(0, 1fr) minmax(340px, 420px);
              gap: 28px;
              align-items: stretch;
            }

            .hero {
              display: flex;
              flex-direction: column;
              justify-content: center;
              gap: 18px;
              min-height: 420px;
            }

            .eyebrow {
              font-size: 12px;
              font-weight: 800;
              letter-spacing: 0.12em;
              text-transform: uppercase;
              color: var(--accent);
            }

            h1 {
              margin: 0;
              font-size: clamp(34px, 5vw, 60px);
              line-height: 1.02;
              font-weight: 800;
              letter-spacing: -0.05em;
            }

            .subtitle {
              max-width: 520px;
              color: var(--text-muted);
              font-size: 16px;
              line-height: 1.75;
            }

            .meta-grid {
              display: grid;
              grid-template-columns: repeat(2, minmax(0, 1fr));
              gap: 12px;
              width: min(560px, 100%);
              padding-top: 8px;
            }

            .meta-item {
              padding: 16px;
              border-radius: 18px;
              background: rgba(255, 255, 255, 0.05);
              border: 1px solid var(--panel-border);
            }

            .meta-label {
              font-size: 12px;
              color: var(--text-soft);
              letter-spacing: 0.08em;
              text-transform: uppercase;
            }

            .meta-value {
              display: block;
              margin-top: 10px;
              font-size: 14px;
              font-weight: 700;
              line-height: 1.55;
            }

            .login-card {
              padding: 24px;
              border-radius: var(--radius-lg);
              background: rgba(5, 14, 23, 0.62);
              border: 1px solid var(--panel-border-strong);
              box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
            }

            .login-card h2 {
              margin: 0 0 8px;
              font-size: 30px;
              font-weight: 800;
              letter-spacing: -0.04em;
            }

            .login-copy {
              margin: 0 0 16px;
              color: var(--text-muted);
              font-size: 13px;
              line-height: 1.7;
            }

            .feedback {
              margin: 0 0 16px;
              padding: 12px 14px;
              border-radius: 16px;
              border: 1px solid var(--panel-border);
              font-size: 13px;
              line-height: 1.6;
            }

            .feedback.error {
              background: var(--danger-soft);
              border-color: rgba(255, 107, 107, 0.28);
              color: #ffb4b4;
            }

            .feedback.success {
              background: var(--success-soft);
              border-color: rgba(64, 201, 135, 0.28);
              color: #a3efc6;
            }

            form {
              display: flex;
              flex-direction: column;
              gap: 14px;
            }

            label {
              display: flex;
              flex-direction: column;
              gap: 8px;
              font-size: 12px;
              color: var(--text-muted);
              letter-spacing: 0.08em;
              text-transform: uppercase;
            }

            input {
              width: 100%;
              min-height: 50px;
              padding: 13px 16px;
              border: 1px solid var(--panel-border);
              border-radius: 16px;
              font-size: 14px;
              color: var(--text-main);
              background: rgba(255, 255, 255, 0.04);
            }

            input::placeholder {
              color: var(--text-muted);
            }

            input:focus {
              outline: none;
              border-color: rgba(95, 209, 194, 0.4);
              box-shadow: 0 0 0 3px rgba(95, 209, 194, 0.12);
            }

            button,
            .oauth-link {
              width: 100%;
              min-height: 48px;
              border-radius: 999px;
              font-size: 14px;
              font-weight: 700;
              letter-spacing: -0.01em;
            }

            button {
              border: 0;
              color: #04111c;
              background: linear-gradient(135deg, var(--accent), var(--accent-strong));
              box-shadow: 0 16px 34px rgba(95, 209, 194, 0.2);
              cursor: pointer;
            }

            .divider {
              display: flex;
              align-items: center;
              gap: 10px;
              margin: 16px 0;
            }

            .divider-line {
              flex: 1;
              height: 1px;
              background: rgba(148, 163, 184, 0.16);
            }

            .divider-text {
              color: var(--text-soft);
              font-size: 12px;
              letter-spacing: 0.08em;
              text-transform: uppercase;
            }

            .oauth-link {
              display: inline-flex;
              align-items: center;
              justify-content: space-between;
              gap: 16px;
              padding: 0 18px;
              color: var(--text-main);
              text-decoration: none;
              background: rgba(255, 255, 255, 0.04);
              border: 1px solid var(--panel-border-strong);
            }

            .oauth-link::after {
              content: "Open";
              font-size: 12px;
              font-weight: 800;
              letter-spacing: 0.08em;
              text-transform: uppercase;
              color: var(--accent);
            }

            @media (max-width: 920px) {
              .layout {
                grid-template-columns: minmax(0, 1fr);
              }

              .hero {
                min-height: auto;
              }
            }

            @media (max-width: 640px) {
              .page {
                padding: 12px;
              }

              .panel {
                padding: 18px;
                border-radius: 28px;
              }

              .meta-grid {
                grid-template-columns: 1fr;
              }

              .login-card {
                padding: 18px;
              }

              .login-card h2 {
                font-size: 26px;
              }
            }
          </style>
        </head>
        <body>
          <main class="page">
            <section class="panel">
              <div class="topbar">
                <div class="brand">
                  <span class="brand-mark">MS</span>
                  <span class="brand-name">My Shop Cloud</span>
                </div>
              </div>
              <div class="layout">
                <section class="hero">
                  <span class="eyebrow">OAuth 2.1</span>
                  <h1>Sign in to My Shop Cloud</h1>
                  <p class="subtitle">Use your platform account to continue into the cloud workspace.</p>
                  <div class="meta-grid">
                    <div class="meta-item">
                      <span class="meta-label">Access mode</span>
                      <span class="meta-value">Unified account entry</span>
                    </div>
                    <div class="meta-item">
                      <span class="meta-label">Authentication</span>
                      <span class="meta-value">Username password or GitHub OAuth</span>
                    </div>
                  </div>
                </section>
                <section class="login-card">
                  <h2>Please sign in</h2>
                  <p class="login-copy">Authorization is handled by the cloud identity service.</p>
                  __FEEDBACK__
                  <form method="post" action="/login">
                    <label>
                      Username
                      <input type="text" name="username" autocomplete="username" placeholder="Username" />
                    </label>
                    <label>
                      Password
                      <input type="password" name="password" autocomplete="current-password" placeholder="Password" />
                    </label>
                    <button type="submit">Sign in</button>
                  </form>
                  <div class="divider">
                    <span class="divider-line"></span>
                    <span class="divider-text">OAuth provider</span>
                    <span class="divider-line"></span>
                  </div>
                  <a class="oauth-link" href="/oauth2/authorization/github">GitHub</a>
                </section>
              </div>
            </section>
          </main>
        </body>
        </html>
        """
        .replace("__FEEDBACK__", renderFeedback(feedback, feedbackClass));
  }

  private String renderFeedback(String message, String feedbackClass) {
    if (message == null || message.isBlank()) {
      return "";
    }
    return "<div class=\"feedback %s\">%s</div>"
        .formatted(HtmlEscapeUtils.escape(feedbackClass), HtmlEscapeUtils.escape(message));
  }
}
