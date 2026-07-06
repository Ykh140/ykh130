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

## ما تم إنجازه (أول شريحة من Phase 1)

- شاشة **المنتجات** كاملة: عرض، إضافة، حذف (soft delete)، تنبيه انخفاض المخزون
- قاعدة بيانات محلية حقيقية (SQLDelight) تعمل بدون إنترنت بالكامل
- دعم RTL كامل للعربية بالواجهة

## كيف تفتحه

1. افتح المجلد `Bayan/` في Android Studio (اختر "Open" وليس "Import")
2. انتظر Gradle sync (أول مرة قد تأخذ بضع دقائق لتحميل SQLDelight/Compose)
3. شغّل `androidApp` على محاكي أو جهاز حقيقي

## الخطوة التالية المقترحة

- شاشة **العملاء والموردين** (نفس نمط Product تمامًا: Repository + ViewModel + Screen)
- ثم **فاتورة البيع** (تربط المنتجات بالعملاء وتُنقص الكمية تلقائيًا)
