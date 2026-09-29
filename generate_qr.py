import sys
import qrcode

def main():
    if len(sys.argv) > 1:
        url = sys.argv[1]
    else:
        url = input("Enter the download link or website URL for HAZORA app: ").strip()

    if not url:
        print("Error: No URL provided.")
        return

    output_filename = "hazora_app_qr.png"

    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_H,
        box_size=10,
        border=4,
    )
    qr.add_data(url)
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    img.save(output_filename)
    print(f"Success! Generated QR code image saved to: {output_filename}")
    print(f"Encoded URL: {url}")

if __name__ == "__main__":
    main()
