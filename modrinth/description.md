# ZeroLag

**Ultra input lag reducer for Minecraft 26.3.** What you see on screen follows your mouse and keyboard as closely as possible, without anything becoming faster than vanilla.

## Why vanilla feels delayed

Every frame, Minecraft reads your input first and *then* waits: for the GPU to finish the previous frame, for VSync, for a free swapchain image. By the time the frame reaches the screen, the input it was built from is already old. On top of that, clicks and key presses are only handled on the next game tick, which comes 20 times a second, so a click can sit for up to 50 ms before anything happens.

ZeroLag goes after both.

## Features

- **Frame Pacing: Smart** (default): measures how long each frame waits for the GPU and the display, and spends that time asleep *before* input is read instead of after. It wakes up earlier on frames that run a game tick and keeps a small, self-tuning safety margin, so frames are not late. Input is read just in time, and FPS did not drop in testing.
- **Frame Pacing: Strict**: every frame waits for the GPU to finish before the next one starts, so nothing is ever queued. Without VSync this gives the lowest latency, at the cost of FPS. With VSync, Smart works better.
- **Late Input**: reads the mouse and keyboard once more right before the camera is placed, after the game ticks and any swapchain wait.
- **Instant Actions**: hotbar, inventory, drop, swap hands and perspective keys act on the frame you press them. In **Full** mode, attack, use and pick block do too, aimed at exactly what that frame shows.
- **Catch Quick Taps**: a jump, sneak or movement press shorter than one tick is normally lost. ZeroLag keeps it for the next tick.
- **Low Latency VSync** (Vulkan): two swapchain images instead of three when VSync is on. If your GPU can't keep up, FPS drops to half your refresh rate, so switch back to Vanilla in that case. **Mailbox** is also available for tear-free VSync with the lowest latency, if your driver supports it.
- **Live status**: the settings screen shows your renderer, present mode and how old your input is when the frame is handed to the display. Bind the toggle key and watch the number change.

## Measured

Time from reading input to handing the frame to the display, on a laptop with a 144 Hz screen, VSync on, unlimited FPS:

| Renderer | Vanilla | ZeroLag | FPS |
|---|---|---|---|
| OpenGL | 6.9 ms | 2.5–3.3 ms | 144 → 144 |
| Vulkan | 6.9 ms | 3.3 ms | 144 → 144 |

This is not the whole click-to-photon time (your monitor adds its own), but it is the part ZeroLag can change. With VSync off and nothing capping the frame rate, frames never wait, so there is nothing to move and the numbers stay the same. ZeroLag helps most when the GPU or VSync is the bottleneck, and VSync is on by default in Minecraft.

## Fair play

- Nothing gets faster than vanilla: held mining and repeated placing still run once per tick, and attack cooldown, reach and break times are untouched.
- No packets are added or reordered.
- On multiplayer servers, attack and use still wait for the tick by default, because a hit that lands between ticks can look unusual to some anti-cheats. Turn on **Instant Combat on Servers** if you accept that risk.

## Settings

Press **F8** in game (rebindable) or open the settings from [Mod Menu](https://modrinth.com/mod/modmenu). There is also a "Toggle ZeroLag" key for quick comparisons; it is unbound by default. Settings are stored in `config/zerolag.json`.

| Setting | Default |
|---|---|
| Frame Pacing | Smart |
| VSync Mode | Low Latency |
| Late Input | On |
| Catch Quick Taps | On |
| Instant Actions | Full |
| Instant Combat on Servers | Off |

## Requirements

- Minecraft 26.3 with Fabric Loader 0.20.1 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25
- Client only: servers don't need it.

## Compatibility

Works with both renderers in 26.3 (OpenGL and Vulkan). Tested together with [Sodium](https://modrinth.com/mod/sodium) 0.9.3-beta.1 and Mod Menu 21.0.0.

Available in English and Turkish.

<details>
<summary>Türkçe</summary>

### ZeroLag

**Minecraft 26.3 için ultra input lag azaltıcı.** Ekranda gördüğün şey fareni ve klavyeni olabildiğince yakından takip eder; ama hiçbir şey vanilladan hızlı olmaz.

### Vanilla neden gecikmeli hissettirir?

Minecraft her karede önce girdini okur, *sonra* bekler: GPU'nun önceki kareyi bitirmesini, VSync'i, boş bir swapchain görüntüsünü. Kare ekrana ulaştığında, onu oluşturan girdi çoktan eskimiş olur. Üstüne, tıklamalar ve tuş basışları ancak bir sonraki oyun tick'inde işlenir. Tick saniyede 20 kez geldiği için bir tıklama, bir şey olmadan önce 50 ms'ye kadar bekleyebilir.

ZeroLag ikisini de hedef alır.

### Özellikler

- **Kare Zamanlaması: Akıllı** (varsayılan): her karenin GPU'yu ve ekranı ne kadar beklediğini ölçer ve bu süreyi girdi okunduktan sonra değil, *önce* uyuyarak geçirir. Oyun tick'i çalışacak karelerde erken uyanır ve kendini ayarlayan küçük bir güvenlik payı bırakır, böylece kareler gecikmez. Girdi tam zamanında okunur; testlerde FPS düşmedi.
- **Kare Zamanlaması: Katı**: her kare, bir sonraki başlamadan önce GPU'nun işini bitirmesini bekler; hiçbir kare kuyruğa girmez. VSync kapalıyken en düşük gecikme budur, ama FPS düşer. VSync açıkken Akıllı mod daha iyidir.
- **Geç Girdi Okuma**: fareyi ve klavyeyi, kamera yerleştirilmeden hemen önce, oyun tick'lerinden ve swapchain beklemesinden sonra bir kez daha okur.
- **Anında Aksiyon**: hotbar, envanter, eşya atma, el değiştirme ve kamera tuşları bastığın karede çalışır. **Tam** modda saldırı, kullanma ve blok seçme de öyle çalışır ve tam olarak o karede gördüğün hedefe uygulanır.
- **Kısa Basışları Yakala**: bir tick'ten kısa süren zıplama, eğilme ya da hareket basışı normalde kaybolur. ZeroLag onu bir sonraki tick'e taşır.
- **Düşük Gecikmeli VSync** (Vulkan): VSync açıkken üç yerine iki swapchain görüntüsü kullanır. GPU'n yetişemezse FPS yenileme hızının yarısına düşer; o durumda Vanilla'ya dön. Sürücün destekliyorsa yırtılmasız ve en düşük gecikmeli VSync için **Mailbox** da var.
- **Canlı durum**: ayar ekranı renderer'ı, sunum modunu ve kare ekrana teslim edildiğinde girdinin ne kadar eski olduğunu gösterir. Aç/kapat tuşunu atayıp sayının değişmesini izleyebilirsin.

### Ölçüm

Girdinin okunmasından karenin ekrana teslim edilmesine kadar geçen süre; 144 Hz ekranlı bir laptop, VSync açık, FPS sınırsız:

| Renderer | Vanilla | ZeroLag | FPS |
|---|---|---|---|
| OpenGL | 6.9 ms | 2.5–3.3 ms | 144 → 144 |
| Vulkan | 6.9 ms | 3.3 ms | 144 → 144 |

Bu, tıklamadan ekrana kadar geçen sürenin tamamı değil (monitör kendi gecikmesini ekler), ama ZeroLag'in değiştirebildiği kısım bu. VSync kapalıyken ve FPS'i hiçbir şey sınırlamıyorken kareler zaten beklemez; taşınacak bir bekleme olmadığından sayılar aynı kalır. ZeroLag en çok GPU'nun ya da VSync'in darboğaz olduğu durumlarda işe yarar ve VSync, Minecraft'ta varsayılan olarak açıktır.

### Adil oyun

- Hiçbir şey vanilladan hızlı olmaz: basılı tutarak kazma ve art arda blok koyma yine tick başına bir kez çalışır; saldırı bekleme süresi, erişim mesafesi ve kırma süreleri değişmez.
- Sunucuya fazladan paket gönderilmez, paketlerin sırası değişmez.
- Çok oyunculu sunucularda saldırı ve kullanma varsayılan olarak yine tick'i bekler, çünkü tick'ler arasına düşen bir vuruş bazı anti-cheat'lere alışılmadık görünebilir. Bu riski kabul ediyorsan **Sunucularda Anında Saldırı** ayarını açabilirsin.

### Ayarlar

Oyunda **F8**'e bas (tuş değiştirilebilir) ya da ayarları Mod Menu'den aç. Hızlı karşılaştırma için bir "ZeroLag'i aç/kapat" tuşu da var; varsayılan olarak atanmamıştır. Ayarlar `config/zerolag.json` dosyasında saklanır.

| Ayar | Varsayılan |
|---|---|
| Kare Zamanlaması | Akıllı |
| VSync Modu | Düşük Gecikme |
| Geç Girdi Okuma | Açık |
| Kısa Basışları Yakala | Açık |
| Anında Aksiyon | Tam |
| Sunucularda Anında Saldırı | Kapalı |

### Gereksinimler

- Minecraft 26.3 ve Fabric Loader 0.20.1 veya üstü
- Fabric API
- Java 25
- Yalnızca istemci: sunucuya kurulması gerekmez.

### Uyumluluk

26.3'ün iki renderer'ında da (OpenGL ve Vulkan) çalışır. Sodium 0.9.3-beta.1 ve Mod Menu 21.0.0 ile birlikte test edildi.

</details>
