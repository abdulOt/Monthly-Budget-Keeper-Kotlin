create extension if not exists "pgcrypto";

create table if not exists public.user_settings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  setup_completed boolean not null default false,
  region text not null default 'Pakistan',
  currency text not null default 'PKR',
  monthly_income numeric not null default 0,
  monthly_budget_target numeric not null default 0,
  main_financial_goal text not null default 'Track spending',
  cycle_type text not null default 'MONTHLY',
  cycle_start_date date not null default current_date,
  next_cycle_date date not null default (current_date + interval '1 month')::date,
  carry_forward_remaining_budget boolean not null default false,
  notifications_enabled boolean not null default false,
  privacy_mode_enabled boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.user_settings
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists setup_completed boolean not null default false,
  add column if not exists region text not null default 'Pakistan',
  add column if not exists currency text not null default 'PKR',
  add column if not exists monthly_income numeric not null default 0,
  add column if not exists monthly_budget_target numeric not null default 0,
  add column if not exists main_financial_goal text not null default 'Track spending',
  add column if not exists cycle_type text not null default 'MONTHLY',
  add column if not exists cycle_start_date date not null default current_date,
  add column if not exists next_cycle_date date not null default (current_date + interval '1 month')::date,
  add column if not exists carry_forward_remaining_budget boolean not null default false,
  add column if not exists notifications_enabled boolean not null default false,
  add column if not exists privacy_mode_enabled boolean not null default false,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

do $$
declare
  id_data_type text;
  created_at_data_type text;
  updated_at_data_type text;
begin
  select data_type
  into id_data_type
  from information_schema.columns
  where table_schema = 'public'
    and table_name = 'user_settings'
    and column_name = 'id';

  if id_data_type is null then
    alter table public.user_settings
      add column id uuid default gen_random_uuid();
  elsif id_data_type <> 'uuid' then
    execute $sql$
      update public.user_settings
      set id = gen_random_uuid()::text
      where id is null
         or trim(id::text) = ''
         or id::text !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
    $sql$;

    alter table public.user_settings
      alter column id type uuid using id::uuid;

    alter table public.user_settings
      alter column id set default gen_random_uuid();
  end if;

  select data_type
  into created_at_data_type
  from information_schema.columns
  where table_schema = 'public'
    and table_name = 'user_settings'
    and column_name = 'created_at';

  if created_at_data_type <> 'timestamp with time zone' then
    alter table public.user_settings
      alter column created_at type timestamptz
      using case
        when created_at is null then now()
        when pg_typeof(created_at)::text = 'bigint' then to_timestamp(created_at::bigint / 1000.0)
        when pg_typeof(created_at)::text = 'integer' then to_timestamp(created_at::integer / 1000.0)
        when pg_typeof(created_at)::text = 'double precision' then to_timestamp(created_at::double precision / 1000.0)
        else created_at::timestamptz
      end;
  end if;

  select data_type
  into updated_at_data_type
  from information_schema.columns
  where table_schema = 'public'
    and table_name = 'user_settings'
    and column_name = 'updated_at';

  if updated_at_data_type <> 'timestamp with time zone' then
    alter table public.user_settings
      alter column updated_at type timestamptz
      using case
        when updated_at is null then now()
        when pg_typeof(updated_at)::text = 'bigint' then to_timestamp(updated_at::bigint / 1000.0)
        when pg_typeof(updated_at)::text = 'integer' then to_timestamp(updated_at::integer / 1000.0)
        when pg_typeof(updated_at)::text = 'double precision' then to_timestamp(updated_at::double precision / 1000.0)
        else updated_at::timestamptz
      end;
  end if;
end
$$;

do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'user_settings'
      and column_name = 'region_code'
  ) then
    execute $sql$
      update public.user_settings
      set region = case
        when region is null or btrim(region) = '' or region = 'Pakistan' then
          case upper(region_code)
            when 'PAKISTAN' then 'Pakistan'
            when 'GERMANY' then 'Germany'
            when 'UNITED_STATES' then 'United States'
            when 'UNITED_KINGDOM' then 'United Kingdom'
            else initcap(replace(region_code, '_', ' '))
          end
        else region
      end
    $sql$;
  end if;

  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'user_settings'
      and column_name = 'financial_goal'
  ) then
    execute $sql$
      update public.user_settings
      set main_financial_goal = case
        when main_financial_goal is null or btrim(main_financial_goal) = '' or main_financial_goal = 'Track spending'
          then coalesce(nullif(btrim(financial_goal), ''), 'Track spending')
        else main_financial_goal
      end
    $sql$;
  end if;

  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'user_settings'
      and column_name = 'main_financial_goal'
  ) then
    execute $sql$
      update public.user_settings
      set main_financial_goal = coalesce(nullif(btrim(main_financial_goal), ''), 'Track spending')
    $sql$;
  end if;
end
$$;

alter table public.user_settings
  alter column user_id set not null,
  alter column setup_completed set default false,
  alter column region set default 'Pakistan',
  alter column currency set default 'PKR',
  alter column monthly_income set default 0,
  alter column monthly_budget_target set default 0,
  alter column main_financial_goal set default 'Track spending',
  alter column cycle_type set default 'MONTHLY',
  alter column cycle_start_date set default current_date,
  alter column next_cycle_date set default (current_date + interval '1 month')::date,
  alter column carry_forward_remaining_budget set default false,
  alter column notifications_enabled set default false,
  alter column privacy_mode_enabled set default false,
  alter column created_at set default now(),
  alter column updated_at set default now();

update public.user_settings
set
  setup_completed = coalesce(setup_completed, false),
  region = coalesce(nullif(btrim(region), ''), 'Pakistan'),
  currency = coalesce(nullif(btrim(currency), ''), 'PKR'),
  monthly_income = coalesce(monthly_income, 0),
  monthly_budget_target = coalesce(monthly_budget_target, 0),
  main_financial_goal = coalesce(nullif(btrim(main_financial_goal), ''), 'Track spending'),
  cycle_type = coalesce(nullif(btrim(cycle_type), ''), 'MONTHLY'),
  cycle_start_date = coalesce(cycle_start_date, current_date),
  next_cycle_date = coalesce(next_cycle_date, (current_date + interval '1 month')::date),
  carry_forward_remaining_budget = coalesce(carry_forward_remaining_budget, false),
  notifications_enabled = coalesce(notifications_enabled, false),
  privacy_mode_enabled = coalesce(privacy_mode_enabled, false),
  created_at = coalesce(created_at, now()),
  updated_at = coalesce(updated_at, now());

alter table public.user_settings
  alter column setup_completed set not null,
  alter column region set not null,
  alter column currency set not null,
  alter column monthly_income set not null,
  alter column monthly_budget_target set not null,
  alter column main_financial_goal set not null,
  alter column cycle_type set not null,
  alter column cycle_start_date set not null,
  alter column next_cycle_date set not null,
  alter column carry_forward_remaining_budget set not null,
  alter column notifications_enabled set not null,
  alter column privacy_mode_enabled set not null,
  alter column created_at set not null,
  alter column updated_at set not null;

create unique index if not exists idx_user_settings_user_id
  on public.user_settings(user_id);

create or replace function public.set_user_settings_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  if new.created_at is null then
    new.created_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_user_settings_updated_at on public.user_settings;
create trigger trg_user_settings_updated_at
before insert or update on public.user_settings
for each row
execute function public.set_user_settings_updated_at();

alter table public.user_settings enable row level security;

drop policy if exists "user_settings_select_own" on public.user_settings;
drop policy if exists "user_settings_insert_own" on public.user_settings;
drop policy if exists "user_settings_update_own" on public.user_settings;
drop policy if exists "user_settings_delete_own" on public.user_settings;
drop policy if exists "user_settings_all_own" on public.user_settings;

create policy "user_settings_select_own"
on public.user_settings
for select
to authenticated
using (auth.uid() = user_id);

create policy "user_settings_insert_own"
on public.user_settings
for insert
to authenticated
with check (auth.uid() = user_id);

create policy "user_settings_update_own"
on public.user_settings
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "user_settings_delete_own"
on public.user_settings
for delete
to authenticated
using (auth.uid() = user_id);
