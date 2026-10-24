# Urlbit Postman Collection & Environment

This directory contains ready-to-import Postman configuration files for testing the Urlbit API.

## Files
1. **`urlbit.postman_collection.json`** — The collection containing all API endpoints organized by folder:
   - **Health & Docs:** `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus`, `/v3/api-docs`
   - **Authentication & Profile:** Signup, Login, Refresh Token Rotation, Logout, Me Profile (`/api/v1/auth/*`)
   - **Link Management:** Create (Guest & Authenticated), Get by ID, List, Update, Soft Delete (`/api/v1/urls/*`)
   - **Click Analytics:** Aggregated Daily Counts & Top Referrer Hosts (`/api/v1/analytics/urls/{id}`)
   - **Public Redirect:** `GET /{code}` (HTTP 302 / 404)
2. **`urlbit_local.postman_environment.json`** — Environment configuration pointing `baseUrl` to `http://localhost:8080`.

---

## How to Import into Postman
1. Open **Postman**.
2. Click the **Import** button in the top-left corner.
3. Drag and drop both files from `docs/postman/`:
   - `urlbit.postman_collection.json`
   - `urlbit_local.postman_environment.json`
4. In the top-right environment selector in Postman, select **"Urlbit Local Environment"**.
5. Start making requests!

### Auto-Chaining
When you run **"Create Short URL"**, a post-response test script automatically sets `{{lastCreatedId}}` and `{{lastCreatedCode}}` in Postman, so the subsequent **"Get by ID"**, **"Update"**, **"Delete"**, and **"Redirect"** requests work immediately without manual copy-pasting.
