# Supabase Setup Guide For Monthly Budget Keeper

This guide matches the current Android app codebase exactly.

The app already supports:

- email and password sign up
- email and password login
- forgot password and in-app reset
- Google login with an ID token exchange
- persistent Supabase session restore on app reopen
- logout
- authenticated Room-to-Supabase sync with `Sync now`
- reinstall restore after sign-in and sync

## 1. Where to find the Supabase URL

1. Open your Supabase project.
2. Go to `Settings > API`.
3. Copy the value labeled `Project URL`.

It looks like:

```text
https://your-project-ref.supabase.co
```

## 2. Where to find the anon key

1. Open the same page: `Settings > API`.
2. Copy the key labeled `anon public`.

Use the full key string exactly as shown.

## 3. What to put in `local.properties`

Open the root project file named `local.properties` and add:

```properties
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
SUPABASE_REDIRECT_SCHEME=monthlybudgetkeeper
SUPABASE_REDIRECT_HOST=auth
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID
```

Notes:

- `SUPABASE_REDIRECT_SCHEME` and `SUPABASE_REDIRECT_HOST` combine into `monthlybudgetkeeper://auth`
- `GOOGLE_WEB_CLIENT_ID` must be the Web OAuth client ID, not the Android client ID

## 4. How to enable Email auth

1. In Supabase, go to `Authentication > Providers`.
2. Open `Email`.
3. Enable the Email provider.
4. Enable signups if you want users to create accounts in the app.

## 5. How to temporarily disable email confirmation for testing

If you want sign up to create an immediate session during testing:

1. Go to `Authentication > Providers > Email`.
2. Turn off the option that requires email confirmation.
3. Save changes.

With confirmation disabled:

- sign up should log the user in immediately

With confirmation enabled:

- Supabase sends a verification email first
- the user must open the link before login is fully usable

## 6. How to add redirect URL `monthlybudgetkeeper://auth`

1. Go to `Authentication > URL Configuration`.
2. Set `Site URL` to a normal HTTPS value such as:

```text
https://budgetkeeper.app
```

3. In `Redirect URLs`, add:

```text
monthlybudgetkeeper://auth
```

Important:

- do not use `monthlybudgetkeeper://auth` as the Site URL
- it must be in the Redirect URL allow-list

## 7. How to enable Google provider

1. Go to `Authentication > Providers`.
2. Open `Google`.
3. Enable the provider.
4. Save after entering the Google credentials from your Google Cloud project.

You will need:

- Google client ID
- Google client secret

These come from the Google Web OAuth client.

## 8. How to configure Google OAuth Web Client ID

### In Google Cloud Console

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create or choose a project.
3. Configure the OAuth consent screen.
4. Go to `APIs & Services > Credentials`.
5. Create an OAuth client of type `Web application`.
6. Copy the client ID.

Put that value into:

```properties
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID
```

### In Supabase

1. Open `Authentication > Providers > Google`.
2. Paste the same Google Web client ID.
3. Paste the matching Google client secret.

### Android note

This app uses Google ID token login, then exchanges that token with Supabase.
The Android app should use the Web client ID in `local.properties`.

## 9. How to run the SQL migration

1. Open Supabase.
2. Go to `SQL Editor`.
3. Open the file:

[supabase_migration.sql](/E:/Talent/Budget%20keeper/supabase_migration.sql:1)

4. Copy the entire file contents.
5. Paste into the SQL Editor.
6. Run it once.

This migration:

- creates all app tables used by sync
- adds `user_id` ownership to all user-scoped tables
- creates `user_id` indexes
- enables Row Level Security
- creates per-user policies
- adds `updated_at` handling with triggers

## 10. How to test signup

1. Fill in `local.properties`.
2. Sync the Gradle project.
3. Run the app.
4. Open `Create account`.
5. Enter full name, email, and password.

Expected results:

- if email confirmation is disabled, the user signs in immediately
- if email confirmation is enabled, Supabase sends a verification email

## 11. How to test login

1. Open the app.
2. Go to `Log in`.
3. Enter the same email and password used for signup.

Expected result:

- user lands in the authenticated app flow
- dashboard opens after the session check finishes

## 12. How to test logout

1. Open `Settings`.
2. Tap `Log out`.

Expected result:

- Supabase session is cleared
- local user-scoped finance data is cleared safely
- app returns to the unauthenticated flow

## 13. How to test password reset

1. Open `Forgot password`.
2. Enter the email address.
3. Submit the reset request.
4. Open the reset email on the same device if possible.
5. Tap the link.

Expected result:

- the link opens `monthlybudgetkeeper://auth`
- the app enters recovery mode
- the reset screen lets the user set a new password

If that does not happen:

- verify the redirect URL allow-list includes `monthlybudgetkeeper://auth`
- verify the Android manifest deep link is present

## 14. How to test Google login

1. Make sure `GOOGLE_WEB_CLIENT_ID` is set in `local.properties`.
2. Make sure the Google provider is enabled in Supabase.
3. Make sure the Web client ID and secret are correct in Supabase.
4. Run the app.
5. Tap `Continue with Google`.

Expected result:

- Google account picker opens
- app receives an ID token
- Supabase creates or restores the session
- authenticated flow opens

## 15. How to test `Sync now`

1. Log in.
2. Add a few transactions, budgets, or finance records.
3. Open `Settings`.
4. Tap `Sync now`.

Expected result:

- sync state changes to syncing, then success
- records are uploaded with the authenticated `user_id`

## 16. How to test uninstall/reinstall restore

1. Log in.
2. Add some finance data.
3. Tap `Sync now`.
4. Close the app.
5. Uninstall the app.
6. Reinstall it.
7. Log in again.

Expected result:

- Supabase restores the session or a new login recreates it
- remote snapshot downloads after authentication
- data is merged into Room and becomes visible again

Important limitation:

- structured finance data restores correctly
- receipt attachment rows restore as metadata only
- the actual local receipt files do not currently sync to cloud storage, so those files themselves will not survive uninstall/reinstall yet

## 17. Common errors and fixes

### Error: `Add your Supabase keys to local.properties before signing in`

Fix:

- add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to `local.properties`
- rebuild the app

### Error: Google sign-in opens but fails immediately

Fix:

- confirm `GOOGLE_WEB_CLIENT_ID` is the Web client ID
- confirm the Google provider is enabled in Supabase
- confirm the client ID and secret in Supabase match Google Cloud

### Error: Password reset email arrives but app does not enter reset mode

Fix:

- confirm `monthlybudgetkeeper://auth` is in Supabase Redirect URLs
- confirm the manifest intent filter uses scheme `monthlybudgetkeeper` and host `auth`

### Error: Signup works but user is told to verify email

Fix:

- this is expected when email confirmation is enabled
- disable email confirmation temporarily in Supabase if you want faster testing

### Error: Sync now fails while logged out

Fix:

- sign in first
- sync is intentionally blocked when there is no authenticated user

### Error: Data does not restore after reinstall

Fix:

1. verify you tapped `Sync now` before uninstalling
2. verify the SQL migration ran successfully
3. verify RLS policies exist
4. verify you logged back into the same Supabase account

### Error: Rows appear in Supabase but not in the app after login

Fix:

- confirm all rows have the correct `user_id`
- confirm the account used in the app matches those rows
- confirm RLS is enabled and the policies allow owner access

### Error: `monthlybudgetkeeper://auth` never opens the app

Fix:

- confirm the Android manifest includes the deep-link intent filter
- confirm the installed app package is the one built from this project
- confirm the redirect URL in Supabase is exactly `monthlybudgetkeeper://auth`
