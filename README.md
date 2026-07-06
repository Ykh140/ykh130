# Bayan (بيان) — MVP

مشروع Kotlin Multiplatform (KMP) + SQLDelight + Jetpack Compose، حسب القرارات المتفق عليها:
- يعمل حاليًا على Android، والبنية جاهزة لإضافة Desktop/iOS لاحقًا بدون إعادة كتابة قاعدة البيانات
- Offline-first بالكامل: كل البيانات تُخزَّن محليًا فورًا عبر SQLDelight
- كل جدول يحتوي `businessId` لدعم تعدد الأنشطة التجارية مستقبلاً (ميزة "رجل الأعمال")

## بنية المشروع

```
Bayan/
├── settings.gradle.kts
├── build.gradle.kts
├── shared/                          ← المنطق المشترك (يُعاد استخدامه لاحقًا مع Desktop/iOS)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── sqldelight/com/bayan/app/db/
│       │   │   ├── Business.sq      ← جدول الأنشطة التجارية
│       │   │   ├── Product.sq       ← جدول المنتجات
│       │   │   └── Customer.sq      ← جدول العملاء/الموردين
│       │   └── kotlin/com/bayan/app/
│       │       ├── domain/
│       │       │   ├── model/Product.kt
│       │       │   └── repository/ProductRepository.kt
│       │       └── data/
│       │           ├── DatabaseDriverFactory.kt   (expect)
│       │           └── repository/ProductRepositoryImpl.kt
│       └── androidMain/
│           └── kotlin/com/bayan/app/data/DatabaseDriverFactory.kt  (actual)
└── androidApp/                       ← واجهة Android فقط (Compose)
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/styles.xml
        └── kotlin/com/bayan/app/android/
            ├── MainActivity.kt
            └── products/
                ├── ProductListViewModel.kt
                └── ProductListScreen.kt
```

## ما تم إنجازه (Phase 1)

- شاشة **المنتجات** كاملة: عرض، إضافة، حذف (soft delete)، تنبيه انخفاض المخزون
- شاشة **العملاء والموردين** (نفس الجدول، مفصولين بـ type): عرض، إضافة، حذف، عرض الرصيد (له/عليه)
- شاشة **فاتورة البيع**: اختيار عميل (اختياري) + إضافة منتجات للسلة + طريقة الدفع (نقد/تحويل/دين)
  - عند تأكيد البيع: تُنشأ الفاتورة وبنودها، **تُنقص كمية كل منتج تلقائيًا**، وإذا كان الدفع بالدين **يُحدَّث رصيد العميل تلقائيًا** — كل هذا داخل عملية قاعدة بيانات واحدة (transaction) لضمان عدم حدوث تحديث جزئي
- تنقل سفلي بين: بيع / المنتجات / العملاء / الموردون
- قاعدة بيانات محلية حقيقية (SQLDelight) تعمل بدون إنترنت بالكامل
- دعم RTL كامل للعربية بالواجهة

## الخطوة التالية المقترحة

- شاشة **المصروفات**
- **Dashboard** بسيط (الرصيد، أرباح اليوم، ديون مستحقة، منتجات قليلة)
- قائمة **الفواتير السابقة** مع إمكانية فتح فاتورة وعرض تفاصيلها
