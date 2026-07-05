create extension if not exists "pgcrypto";

create or replace function public.epoch_millis_now()
returns bigint
language sql
stable
as $$
  select (extract(epoch from now()) * 1000)::bigint;
$$;

create or replace function public.set_epoch_updated_at()
returns trigger
language plpgsql
as $$
begin
  if tg_op = 'INSERT' then
    if new.created_at is null or new.created_at = 0 then
      new.created_at := public.epoch_millis_now();
    end if;
  end if;
  new.updated_at := public.epoch_millis_now();
  return new;
end;
$$;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  user_id uuid not null unique references auth.users(id) on delete cascade,
  email text not null,
  full_name text not null,
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now(),
  check (id = user_id)
);

create table if not exists public.user_settings (
  id text primary key,
  user_id uuid not null unique references auth.users(id) on delete cascade,
  onboarding_completed boolean not null default false,
  setup_completed boolean not null default false,
  currency text not null,
  region_code text not null default 'PAKISTAN',
  monthly_income double precision not null default 0,
  monthly_budget_target double precision not null default 0,
  main_financial_goal text not null default 'Track spending',
  cycle_type text not null default 'MONTHLY',
  cycle_start_date date not null default current_date,
  next_cycle_date date not null default (current_date + interval '1 month')::date,
  carry_forward_remaining_budget boolean not null default false,
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
);
create index if not exists idx_user_settings_user_id on public.user_settings(user_id);

alter table public.user_settings
  add column if not exists region_code text not null default 'PAKISTAN';
alter table public.user_settings
  add column if not exists setup_completed boolean not null default false;
alter table public.user_settings
  add column if not exists monthly_income double precision not null default 0;
alter table public.user_settings
  add column if not exists monthly_budget_target double precision not null default 0;
alter table public.user_settings
  add column if not exists main_financial_goal text not null default 'Track spending';
alter table public.user_settings
  add column if not exists cycle_type text not null default 'MONTHLY';
alter table public.user_settings
  add column if not exists cycle_start_date date not null default current_date;
alter table public.user_settings
  add column if not exists next_cycle_date date not null default (current_date + interval '1 month')::date;
alter table public.user_settings
  add column if not exists carry_forward_remaining_budget boolean not null default false;

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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
);
create index if not exists idx_transactions_user_id on public.transactions(user_id);
create index if not exists idx_transactions_account_remote_id on public.transactions(account_remote_id);
create index if not exists idx_transactions_transfer_remote_id on public.transactions(transfer_remote_id);

create table if not exists public.monthly_budgets (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  month_key text not null,
  total_budget double precision not null,
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
);
create unique index if not exists idx_monthly_budgets_user_month
  on public.monthly_budgets(user_id, month_key);

create table if not exists public.category_budgets (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  month_key text not null,
  category text not null,
  limit_amount double precision not null,
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
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
  created_at bigint not null default public.epoch_millis_now(),
  updated_at bigint not null default public.epoch_millis_now()
);
create index if not exists idx_transaction_rules_user_id on public.transaction_rules(user_id);

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at before insert or update on public.profiles
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_user_settings_updated_at on public.user_settings;
create trigger trg_user_settings_updated_at before insert or update on public.user_settings
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_transactions_updated_at on public.transactions;
create trigger trg_transactions_updated_at before insert or update on public.transactions
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_monthly_budgets_updated_at on public.monthly_budgets;
create trigger trg_monthly_budgets_updated_at before insert or update on public.monthly_budgets
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_category_budgets_updated_at on public.category_budgets;
create trigger trg_category_budgets_updated_at before insert or update on public.category_budgets
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_accounts_updated_at on public.accounts;
create trigger trg_accounts_updated_at before insert or update on public.accounts
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_transfers_updated_at on public.transfers;
create trigger trg_transfers_updated_at before insert or update on public.transfers
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_recurring_items_updated_at on public.recurring_items;
create trigger trg_recurring_items_updated_at before insert or update on public.recurring_items
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_subscription_bills_updated_at on public.subscription_bills;
create trigger trg_subscription_bills_updated_at before insert or update on public.subscription_bills
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_goals_updated_at on public.goals;
create trigger trg_goals_updated_at before insert or update on public.goals
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_debts_updated_at on public.debts;
create trigger trg_debts_updated_at before insert or update on public.debts
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_envelope_budgets_updated_at on public.envelope_budgets;
create trigger trg_envelope_budgets_updated_at before insert or update on public.envelope_budgets
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_paycheck_plans_updated_at on public.paycheck_plans;
create trigger trg_paycheck_plans_updated_at before insert or update on public.paycheck_plans
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_split_transactions_updated_at on public.split_transactions;
create trigger trg_split_transactions_updated_at before insert or update on public.split_transactions
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_receipt_attachments_updated_at on public.receipt_attachments;
create trigger trg_receipt_attachments_updated_at before insert or update on public.receipt_attachments
for each row execute function public.set_epoch_updated_at();

drop trigger if exists trg_transaction_rules_updated_at on public.transaction_rules;
create trigger trg_transaction_rules_updated_at before insert or update on public.transaction_rules
for each row execute function public.set_epoch_updated_at();

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

drop policy if exists "profiles_all_own" on public.profiles;
create policy "profiles_all_own" on public.profiles
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id)
with check (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

drop policy if exists "user_settings_all_own" on public.user_settings;
create policy "user_settings_all_own" on public.user_settings
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "transactions_all_own" on public.transactions;
create policy "transactions_all_own" on public.transactions
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "monthly_budgets_all_own" on public.monthly_budgets;
create policy "monthly_budgets_all_own" on public.monthly_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "category_budgets_all_own" on public.category_budgets;
create policy "category_budgets_all_own" on public.category_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "accounts_all_own" on public.accounts;
create policy "accounts_all_own" on public.accounts
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "transfers_all_own" on public.transfers;
create policy "transfers_all_own" on public.transfers
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "recurring_items_all_own" on public.recurring_items;
create policy "recurring_items_all_own" on public.recurring_items
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "subscription_bills_all_own" on public.subscription_bills;
create policy "subscription_bills_all_own" on public.subscription_bills
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "goals_all_own" on public.goals;
create policy "goals_all_own" on public.goals
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "debts_all_own" on public.debts;
create policy "debts_all_own" on public.debts
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "envelope_budgets_all_own" on public.envelope_budgets;
create policy "envelope_budgets_all_own" on public.envelope_budgets
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "paycheck_plans_all_own" on public.paycheck_plans;
create policy "paycheck_plans_all_own" on public.paycheck_plans
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "split_transactions_all_own" on public.split_transactions;
create policy "split_transactions_all_own" on public.split_transactions
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "receipt_attachments_all_own" on public.receipt_attachments;
create policy "receipt_attachments_all_own" on public.receipt_attachments
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "transaction_rules_all_own" on public.transaction_rules;
create policy "transaction_rules_all_own" on public.transaction_rules
for all to authenticated
using (auth.uid() is not null and auth.uid() = user_id)
with check (auth.uid() is not null and auth.uid() = user_id);
