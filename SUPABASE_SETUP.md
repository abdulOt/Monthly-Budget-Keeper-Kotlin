# Supabase Setup For Budget Keeper

This project already includes:

- Supabase Auth for email/password, password recovery, and Google ID token sign-in
- persistent auth session restore on cold start
- Room-first offline storage
- per-user cloud sync between Room and Supabase

To make that work correctly, the Supabase project must match the Android code.

## 1. Fill In `local.properties`

Add these values to `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
SUPABASE_REDIRECT_SCHEME=monthlybudgetkeeper
SUPABASE_REDIRECT_HOST=auth
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID
```

The app builds its auth redirect URI from the last two values:

```text
monthlybudgetkeeper://auth
```

`GOOGLE_WEB_CLIENT_ID` must be the Google OAuth Web client ID, not the Android client ID.

## 2. Configure Supabase Auth URLs

Open `Authentication > URL Configuration` in Supabase.

Set:

- `Site URL`: your normal website URL, or a placeholder HTTPS URL such as `https://budgetkeeper.app`
- `Redirect URLs`: add `monthlybudgetkeeper://auth`

Important:

- do not set `Site URL` to `monthlybudgetkeeper://auth`
- the custom scheme belongs in the Redirect URL allow-list
- this redirect must match:
  - `SUPABASE_REDIRECT_SCHEME`
  - `SUPABASE_REDIRECT_HOST`
  - the manifest deep link in [AndroidManifest.xml](/E:/Talent/Budget%20keeper/app/src/main/AndroidManifest.xml:1)

## 3. Configure Email Auth

In `Authentication > Providers > Email`:

- enable Email provider
- enable Signups if you want in-app registration
- decide whether email confirmation is required

The app already supports both cases:

- if confirmation is disabled, sign-up may create an immediate session
- if confirmation is enabled, Supabase sends a verification link that redirects back to `monthlybudgetkeeper://auth`

## 4. Configure Password Reset

The app sends password reset links with this redirect:

```text
monthlybudgetkeeper://auth
```

The reset flow depends on:

- [AuthRepository.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/repository/AuthRepository.kt:1)
- [MainActivity.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/MainActivity.kt:1)
- [SessionManager.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/auth/SessionManager.kt:1)

If the reset link opens but recovery mode does not start, the first thing to verify is that the redirect URL in Supabase is exactly the same custom URI the app expects.

## 5. Configure Google Sign-In

This app uses the Google ID token flow, then exchanges that ID token with Supabase.

That means:

- the app does not use a browser OAuth redirect for Google sign-in
- the deep link is still needed for email verification and password recovery
- Google sign-in mainly depends on the Google Web client ID and the Supabase Google provider configuration

### Google Cloud Console

1. Create or reuse a Google Cloud project.
2. Configure the OAuth consent screen.
3. Create:
   - an Android OAuth client for the app package
   - a Web OAuth client
4. Put the Web client ID into:

```properties
GOOGLE_WEB_CLIENT_ID=...
```

### Supabase

In `Authentication > Providers > Google`:

- enable Google
- add the Google client ID and client secret required by Supabase

Android-side flow:

- [GoogleAuthManager.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/utils/GoogleAuthManager.kt:1) requests a Google ID token with Credential Manager
- [AuthRepository.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/repository/AuthRepository.kt:1) calls `signInWith(IDToken)` using provider `Google`

## 6. Create The Supabase Tables

Run this SQL in Supabase SQL Editor:

```sql
create extension if not exists "pgcrypto";

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  user_id uuid not null unique references auth.users(id) on delete cascade,
  email text not null,
  full_name text not null,
  created_at bigint not null,
  updated_at bigint not null,
  check (id = user_id)
);

create table if not exists public.user_settings (
  id text primary key,
  user_id uuid not null unique references auth.users(id) on delete cascade,
  onboarding_completed boolean not null default false,
  currency text not null,
  region_code text not null default 'PAKISTAN',
  dark_mode boolean not null default false,
  profile_name text not null,
  week_start text not null,
  notifications_enabled boolean not null default true,
  bill_reminders_enabled boolean not null default true,
  recurring_reminders_enabled boolean not null default true,
  debt_reminders_enabled boolean not null default true,
  budget_alerts_enabled boolean not null default true,
  goal_reminders_enabled boolean not null default true,
  reminder_lead_days integer not null default 3,
  privacy_mode_enabled boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null
);

create table if not exists public.transactions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  account_remote_id uuid,
  transfer_remote_id uuid,
  title text not null,
  amount double precision not null,
  type text not null,
  category text not null,
  date date not null,
  note text not null default '',
  is_transfer boolean not null default false,
  is_planned boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_transactions_user_id on public.transactions(user_id);
create index if not exists idx_transactions_account_remote_id on public.transactions(account_remote_id);
create index if not exists idx_transactions_transfer_remote_id on public.transactions(transfer_remote_id);

create table if not exists public.monthly_budgets (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  month_key text not null,
  total_budget double precision not null,
  created_at bigint not null,
  updated_at bigint not null
);
create unique index if not exists idx_monthly_budgets_user_month
  on public.monthly_budgets(user_id, month_key);

create table if not exists public.category_budgets (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  month_key text not null,
  category text not null,
  limit_amount double precision not null,
  created_at bigint not null,
  updated_at bigint not null
);
create unique index if not exists idx_category_budgets_user_month_category
  on public.category_budgets(user_id, month_key, category);

create table if not exists public.accounts (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  account_type text not null,
  current_balance double precision not null,
  currency_code text not null,
  institution text not null default '',
  include_in_net_worth boolean not null default true,
  is_archived boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_accounts_user_id on public.accounts(user_id);

create table if not exists public.transfers (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  from_account_remote_id uuid not null,
  to_account_remote_id uuid not null,
  amount double precision not null,
  date date not null,
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_transfers_user_id on public.transfers(user_id);

create table if not exists public.recurring_items (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  account_remote_id uuid,
  title text not null,
  amount double precision not null,
  type text not null,
  category text not null,
  start_date date not null,
  next_due_date date not null,
  end_date date,
  interval text not null,
  note text not null default '',
  auto_create_transaction boolean not null default false,
  is_active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_recurring_items_user_id on public.recurring_items(user_id);

create table if not exists public.subscription_bills (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  account_remote_id uuid,
  name text not null,
  amount double precision not null,
  category text not null,
  due_date date not null,
  next_charge_date date not null,
  billing_cycle text not null,
  note text not null default '',
  reminder_days integer not null default 3,
  is_auto_pay boolean not null default false,
  is_active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_subscription_bills_user_id on public.subscription_bills(user_id);

create table if not exists public.goals (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  linked_account_remote_id uuid,
  name text not null,
  target_amount double precision not null,
  current_amount double precision not null default 0,
  target_date date,
  monthly_contribution double precision not null default 0,
  is_sinking_fund boolean not null default false,
  note text not null default '',
  is_completed boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_goals_user_id on public.goals(user_id);

create table if not exists public.debts (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  account_remote_id uuid,
  name text not null,
  lender text not null,
  total_amount double precision not null,
  remaining_amount double precision not null,
  due_date date,
  interest_rate double precision not null default 0,
  minimum_payment double precision not null default 0,
  planned_payment double precision not null default 0,
  strategy text not null default 'AVALANCHE',
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_debts_user_id on public.debts(user_id);

create table if not exists public.envelope_budgets (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  month_key text not null,
  name text not null,
  category text,
  planned_amount double precision not null,
  rollover_amount double precision not null default 0,
  spent_amount double precision not null default 0,
  created_at bigint not null,
  updated_at bigint not null
);
create unique index if not exists idx_envelope_budgets_user_month_name
  on public.envelope_budgets(user_id, month_key, name);

create table if not exists public.paycheck_plans (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  account_remote_id uuid,
  expected_amount double precision not null,
  payday date not null,
  allocated_amount double precision not null default 0,
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_paycheck_plans_user_id on public.paycheck_plans(user_id);

create table if not exists public.split_transactions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  parent_transaction_remote_id uuid not null,
  title text not null,
  category text not null,
  amount double precision not null,
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_split_transactions_user_id on public.split_transactions(user_id);
create index if not exists idx_split_transactions_parent on public.split_transactions(parent_transaction_remote_id);

create table if not exists public.receipt_attachments (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  transaction_remote_id uuid not null,
  local_uri text not null,
  file_name text not null,
  mime_type text not null,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_receipt_attachments_user_id on public.receipt_attachments(user_id);
create index if not exists idx_receipt_attachments_transaction on public.receipt_attachments(transaction_remote_id);

create table if not exists public.transaction_rules (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  keyword text not null,
  match_field text not null,
  target_type text not null,
  target_category text not null,
  target_account_remote_id uuid,
  priority integer not null default 0,
  is_active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null
);
create index if not exists idx_transaction_rules_user_id on public.transaction_rules(user_id);
```

## 7. Enable RLS

Run this SQL too:

```sql
alter table public.profiles enable row level security;
alter table public.user_settings enable row level security;
alter table public.transactions enable row level security;
alter table public.monthly_budgets enable row level security;
alter table public.category_budgets enable row level security;
alter table public.accounts enable row level security;
alter table public.transfers enable row level security;
alter table public.recurring_items enable row level security;
alter table public.subscription_bills enable row level security;
alter table public.goals enable row level security;
alter table public.debts enable row level security;
alter table public.envelope_budgets enable row level security;
alter table public.paycheck_plans enable row level security;
alter table public.split_transactions enable row level security;
alter table public.receipt_attachments enable row level security;
alter table public.transaction_rules enable row level security;
```

Then create policies:

```sql
create policy "profiles_all_own" on public.profiles
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id)
with check (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

create policy "user_settings_all_own" on public.user_settings
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "transactions_all_own" on public.transactions
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "monthly_budgets_all_own" on public.monthly_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "category_budgets_all_own" on public.category_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "accounts_all_own" on public.accounts
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "transfers_all_own" on public.transfers
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "recurring_items_all_own" on public.recurring_items
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "subscription_bills_all_own" on public.subscription_bills
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "goals_all_own" on public.goals
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "debts_all_own" on public.debts
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "envelope_budgets_all_own" on public.envelope_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "paycheck_plans_all_own" on public.paycheck_plans
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "split_transactions_all_own" on public.split_transactions
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "receipt_attachments_all_own" on public.receipt_attachments
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

create policy "transaction_rules_all_own" on public.transaction_rules
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);
```

## 8. What The App Already Does

The current code already handles:

- Android deep link auth callback in [AndroidManifest.xml](/E:/Talent/Budget%20keeper/app/src/main/AndroidManifest.xml:1)
- cold-start and new-intent handoff in [MainActivity.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/MainActivity.kt:1)
- auth session tracking in [SessionManager.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/auth/SessionManager.kt:1)
- Room-first background sync in [SupabaseSyncManager.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/sync/SupabaseSyncManager.kt:1)
- DTO mapping in [CloudSyncMappers.kt](/E:/Talent/Budget%20keeper/app/src/main/java/com/talent/monthlybudgetkeeper/data/model/CloudSyncMappers.kt:1)

## 9. Secure Sync Assumptions

- every synced table uses `user_id` and is filtered by that same authenticated user id in both code and RLS
- the `profiles` row is additionally constrained so `id = user_id = auth.uid()`
- `user_settings` is the cloud home for synced preferences such as currency, theme, reminders, and privacy mode
- app-lock secrets stay device-local:
  - PIN hash is not stored in Supabase
  - biometric enablement is not stored in Supabase
- receipt attachment rows currently sync metadata only:
  - `transaction_remote_id`
  - local URI string
  - file name
  - MIME type
- if you want actual receipt files to survive reinstall or device change, move attachment binaries to Supabase Storage and keep only the storage path in Postgres

## 10. Final Test Checklist

1. Add the five Supabase and Google keys to `local.properties`.
2. Keep `monthlybudgetkeeper://auth` in the Redirect URL allow-list.
3. Do not use the custom scheme as the Supabase Site URL.
4. Run the SQL for tables and RLS.
5. Enable Email auth.
6. Enable Google auth if needed and add the Web client ID.
7. Install the app.
8. Sign in.
9. Add data locally.
10. Use `Sync now` in Settings.
11. Reinstall the app.
12. Sign in again and confirm the data restores from cloud.
