create extension if not exists "pgcrypto";

begin;

create or replace function public.ensure_user_owned_policies(target_table regclass)
returns void
language plpgsql
as $$
declare
  table_name text := split_part(target_table::text, '.', 2);
  select_policy text := table_name || '_select_own';
  insert_policy text := table_name || '_insert_own';
  update_policy text := table_name || '_update_own';
  delete_policy text := table_name || '_delete_own';
begin
  execute format('alter table %s enable row level security', target_table);
  execute format('alter table %s force row level security', target_table);

  execute format('drop policy if exists %I on %s', select_policy, target_table);
  execute format('drop policy if exists %I on %s', insert_policy, target_table);
  execute format('drop policy if exists %I on %s', update_policy, target_table);
  execute format('drop policy if exists %I on %s', delete_policy, target_table);

  execute format(
    'create policy %I on %s for select to authenticated using (auth.uid() is not null and auth.uid() = user_id)',
    select_policy,
    target_table
  );
  execute format(
    'create policy %I on %s for insert to authenticated with check (auth.uid() is not null and auth.uid() = user_id)',
    insert_policy,
    target_table
  );
  execute format(
    'create policy %I on %s for update to authenticated using (auth.uid() is not null and auth.uid() = user_id) with check (auth.uid() is not null and auth.uid() = user_id)',
    update_policy,
    target_table
  );
  execute format(
    'create policy %I on %s for delete to authenticated using (auth.uid() is not null and auth.uid() = user_id)',
    delete_policy,
    target_table
  );
end;
$$;

create table if not exists public.assets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  asset_type text not null default 'CUSTOM',
  amount double precision not null default 0,
  note text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.liabilities (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  liability_type text not null default 'CUSTOM',
  amount double precision not null default 0,
  note text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.notification_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  event_key text not null,
  last_sent_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, event_key)
);

create index if not exists idx_assets_user_id on public.assets(user_id);
create index if not exists idx_liabilities_user_id on public.liabilities(user_id);
create index if not exists idx_notification_events_user_id on public.notification_events(user_id);

do $$
declare
  target_table text;
begin
  foreach target_table in array array[
    'public.user_settings',
    'public.transactions',
    'public.accounts',
    'public.monthly_budgets',
    'public.category_budgets',
    'public.envelope_budgets',
    'public.paycheck_plans',
    'public.subscription_bills',
    'public.recurring_items',
    'public.goals',
    'public.debts',
    'public.assets',
    'public.liabilities',
    'public.notification_events'
  ]
  loop
    if to_regclass(target_table) is not null then
      execute format('alter table %s alter column user_id set not null', target_table);
      perform public.ensure_user_owned_policies(target_table::regclass);
    end if;
  end loop;
end;
$$;

alter table public.profiles enable row level security;
alter table public.profiles force row level security;

drop policy if exists profiles_select_own on public.profiles;
drop policy if exists profiles_insert_own on public.profiles;
drop policy if exists profiles_update_own on public.profiles;
drop policy if exists profiles_delete_own on public.profiles;

create policy profiles_select_own
on public.profiles
for select
to authenticated
using (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

create policy profiles_insert_own
on public.profiles
for insert
to authenticated
with check (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

create policy profiles_update_own
on public.profiles
for update
to authenticated
using (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id)
with check (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

create policy profiles_delete_own
on public.profiles
for delete
to authenticated
using (auth.uid() is not null and auth.uid() = user_id and auth.uid() = id);

revoke all on public.user_settings from anon;
revoke all on public.transactions from anon;
revoke all on public.accounts from anon;
revoke all on public.monthly_budgets from anon;
revoke all on public.category_budgets from anon;
revoke all on public.envelope_budgets from anon;
revoke all on public.paycheck_plans from anon;
revoke all on public.subscription_bills from anon;
revoke all on public.recurring_items from anon;
revoke all on public.goals from anon;
revoke all on public.debts from anon;
revoke all on public.assets from anon;
revoke all on public.liabilities from anon;
revoke all on public.notification_events from anon;
revoke all on public.profiles from anon;

commit;
