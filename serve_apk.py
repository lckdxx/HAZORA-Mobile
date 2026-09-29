import socket
import http.server
import socketserver
import qrcode
import os

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def main():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    apk_dir = os.path.join(root_dir, "app", "build", "outputs", "apk", "debug")
    apk_file = "app-debug.apk"
    apk_path = os.path.join(apk_dir, apk_file)

    if not os.path.exists(apk_path):
        print(f"Error: APK not found at {apk_path}")
        print("Please build the app first using: gradlew assembleDebug")
        return

    port = 8080
    local_ip = get_local_ip()
    download_url = f"http://{local_ip}:{port}/{apk_file}"

    # Generate QR Code image
    qr = qrcode.QRCode(version=1, box_size=10, border=4)
    qr.add_data(download_url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    qr_output = os.path.join(root_dir, "hazora_app_qr.png")
    img.save(qr_output)

    print("=" * 60)
    print(" HAZORA LOCAL APK DOWNLOAD SERVER")
    print("=" * 60)
    print(f" Local Download URL : {download_url}")
    print(f" QR Code Saved To   : {qr_output}")
    print("-" * 60)
    print(" Scan 'hazora_app_qr.png' with your mobile phone")
    print(" (make sure phone is connected to the same Wi-Fi network).")
    print("-" * 60)
    print(" Press Ctrl+C in terminal to stop serving.")
    print("=" * 60)

    os.chdir(apk_dir)
    handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", port), handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")

if __name__ == "__main__":
    main()
