-- ============================================================================
-- Bayan (بيان) — مخطط قاعدة بيانات Supabase (Postgres)
-- ============================================================================
-- طريقة الاستخدام:
--   1. افتح مشروعك على supabase.com
--   2. من القائمة الجانبية: SQL Editor → New query
--   3. الصق هذا الملف كاملاً ثم اضغط Run
--
-- الفكرة: هذا المخطط "مرآة" لجداول SQLDelight المحلية بالضبط. التطبيق يشتغل
-- محليًا offline-first دائمًا (SQLDelight هو مصدر الحقيقة)، وهذه الجداول تُستخدم
-- بس للمزامنة بين الأجهزة لما يتوفر اتصال إنترنت.
--
-- business_id = auth.uid() دائمًا: كل حساب Supabase يمثّل نشاط تجاري واحد
-- بمرحلة الـ MVP، وهذا يبسّط سياسات RLS (كل مستخدم يشوف بياناته فقط).
-- ============================================================================

-- تفعيل امتداد UUID (مفعّل افتراضيًا بمعظم مشاريع Supabase، هذا السطر للتأكد فقط)
create extension if not exists "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Product
-- ----------------------------------------------------------------------------
create table if not exists public.product (
    id text primary key,
    business_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    barcode text,
    internal_code text,
    category text,
    unit text not null default 'قطعة',
    purchase_price double precision not null default 0.0,
    sale_price double precision not null default 0.0,
    quantity double precision not null default 0.0,
    low_stock_threshold double precision not null default 0.0,
    image_path text,
    is_deleted boolean not null default false,
    created_at bigint not null,
    updated_at bigint not null
);
create index if not exists product_business_idx on public.product(business_id);

-- ----------------------------------------------------------------------------
-- 2. Customer (عملاء وموردون معًا، type يفرّق بينهم)
-- ----------------------------------------------------------------------------
create table if not exists public.customer (
    id text primary key,
    business_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    phone text,
    type text not null, -- 'customer' | 'supplier'
    balance double precision not null default 0.0,
    notes text,
    is_deleted boolean not null default false,
    created_at bigint not null,
    updated_at bigint not null
);
create index if not exists customer_business_idx on public.customer(business_id);

-- ----------------------------------------------------------------------------
-- 3. Invoice + InvoiceItem
-- ----------------------------------------------------------------------------
create table if not exists public.invoice (
    id text primary key,
    business_id uuid not null references auth.users(id) on delete cascade,
    customer_id text,
    total_amount double precision not null,
    payment_method text not null, -- 'cash' | 'transfer' | 'debt'
    is_deleted boolean not null default false,
    created_at bigint not null
);
create index if not exists invoice_business_idx on public.invoice(business_id);
create index if not exists invoice_customer_idx on public.invoice(customer_id);

create table if not exists public.invoice_item (
    id text primary key,
    invoice_id text not null references public.invoice(id) on delete cascade,
    product_id text not null,
    product_name text not null,
    quantity double precision not null,
    unit_price double precision not null,
    unit_cost double precision not null
);
create index if not exists invoice_item_invoice_idx on public.invoice_item(invoice_id);

-- ----------------------------------------------------------------------------
-- 4. Payment (تسديد / دفعة-سلفة لعميل أو مورد)
-- ----------------------------------------------------------------------------
create table if not exists public.payment (
    id text primary key,
    business_id uuid not null references auth.users(id) on delete cascade,
    party_id text not null,
    amount double precision not null,
    balance_delta double precision not null,
    note text,
    created_at bigint not null
);
create index if not exists payment_business_idx on public.payment(business_id);
create index if not exists payment_party_idx on public.payment(party_id);

-- ----------------------------------------------------------------------------
-- 5. Expense
-- ----------------------------------------------------------------------------
create table if not exists public.expense (
    id text primary key,
    business_id uuid not null references auth.users(id) on delete cascade,
    amount double precision not null,
    category text,
    note text,
    is_deleted boolean not null default false,
    created_at bigint not null
);
create index if not exists expense_business_idx on public.expense(business_id);

-- ============================================================================
-- Row Level Security: كل مستخدم يشوف ويعدّل بيانات نشاطه التجاري فقط
-- ============================================================================
alter table public.product enable row level security;
alter table public.customer enable row level security;
alter table public.invoice enable row level security;
alter table public.invoice_item enable row level security;
alter table public.payment enable row level security;
alter table public.expense enable row level security;

-- Product
create policy "product_owner_all" on public.product
    for all using (business_id = auth.uid()) with check (business_id = auth.uid());

-- Customer
create policy "customer_owner_all" on public.customer
    for all using (business_id = auth.uid()) with check (business_id = auth.uid());

-- Invoice
create policy "invoice_owner_all" on public.invoice
    for all using (business_id = auth.uid()) with check (business_id = auth.uid());

-- InvoiceItem (لا يوجد business_id مباشر، نتحقق عبر الفاتورة الأم)
create policy "invoice_item_owner_all" on public.invoice_item
    for all using (
        exists (
            select 1 from public.invoice i
            where i.id = invoice_item.invoice_id and i.business_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1 from public.invoice i
            where i.id = invoice_item.invoice_id and i.business_id = auth.uid()
        )
    );

-- Payment
create policy "payment_owner_all" on public.payment
    for all using (business_id = auth.uid()) with check (business_id = auth.uid());

-- Expense
create policy "expense_owner_all" on public.expense
    for all using (business_id = auth.uid()) with check (business_id = auth.uid());

-- ============================================================================
-- ملاحظة: بعد تشغيل هذا الملف، لازم تتأكد إن الجداول مفعّلة على الـ Data API
-- (Project Settings → Data API → Exposed schemas يجب أن يتضمن "public").
-- بمشاريع Supabase الجديدة بعد 30 مايو 2026 هذا الأمر لازم يكون صريح.
-- ============================================================================
