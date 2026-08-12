# Geçiş ve Devir Notları

Bu dosya, projenin `Apartman_Yonetim_Sistemi`nden kopyalanması ve yeni bir pencerede çalışmaya devam edilmesi üzerine oluşturulmuştur.

## Mevcut Durum
- Proje başarıyla kopyalandı.
- Henüz kod değişikliği yapılmadı.
- Planlama tamamlandı.

## Uygulama Planı (Sıradaki Adımlar)

### 1. Internationalization (i18n) - Çoklu Dil
- [ ] Java dosyalarındaki tüm Türkçe metinleri `strings.xml` içine taşı.
- [ ] XML dosyalarındaki (layout) tüm metinleri `strings.xml` içine taşı.
- [ ] `res/values-nl/strings.xml` oluştur ve Flamanca çevirileri ekle.
- [ ] Para birimini dile göre ayarla (TL / €).

### 2. Aidat Özelliği
- [ ] `DatabaseHelper` sınıfında tablo versiyonunu artır ve `aidat_amount` (Real/Double) sütunu ekle.
- [ ] `Apartment` modeline `aidatAmount` alanını ekle.
- [ ] `ApartmentDetailActivity` içinde Aidat düzenleme ve görüntüleme alanı ekle.
- [ ] Kira ve Aidat'ın ayrı ayrı girilebilmesini sağla.

## Yeni Asistana Talimat
Bu dosyayı gören yeni asistan, yukarıdaki adımları sırasıyla uygulamaya başlayabilir.
