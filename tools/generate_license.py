#!/usr/bin/env python3
"""
ARBCN Admin License & Promo Code Generator Tool
Use this script to generate activation keys after receiving customer payments via DANA.

Format License: ARBCN-<PLAN>-<EXPIRY_MILLIS>-<DEVICE_ID>-<SIGNATURE>
Format Promo:   PROMO-<NAME>-<DAYS>-<SIGNATURE>
"""

import hmac
import hashlib
import time
import argparse
import sys

HMAC_SECRET = "ARBCN_2026_SECURE_LICENSE_SECRET_v1_X9K2M8N4P6".encode('utf-8')

def sign_payload(payload: str) -> str:
    return hmac.new(HMAC_SECRET, payload.encode('utf-8'), hashlib.sha256).hexdigest().upper()

def generate_license(plan: str, device_id: str, days: int = 30) -> str:
    cleaned_device = device_id.strip().upper()
    plan_upper = plan.strip().upper()

    if plan_upper == "LIFETIME":
        expiry = 9223372036854775807 # Long.MAX_VALUE
    else:
        now_ms = int(time.time() * 1000)
        duration_ms = days * 24 * 60 * 60 * 1000
        expiry = now_ms + duration_ms

    payload = f"{plan_upper}|{expiry}|{cleaned_device}"
    signature = sign_payload(payload)[:24]

    return f"ARBCN-{plan_upper}-{expiry}-{cleaned_device}-{signature}"

def generate_promo(code_name: str, days: int = 3) -> str:
    name_clean = code_name.strip().upper().replace(" ", "")
    payload = f"PROMO|{name_clean}|{days}"
    signature = sign_payload(payload)[:16]
    return f"PROMO-{name_clean}-{days}-{signature}"

def main():
    parser = argparse.ArgumentParser(description="ARBCN License & Promo Generator (Admin DANA)")
    subparsers = parser.add_subparsers(dest="command")

    # License subparser
    lic_parser = subparsers.add_parser("license", help="Generate customer license key")
    lic_parser.add_argument("--device", "-d", required=True, help="16-character Device ID copied from customer app")
    lic_parser.add_argument("--plan", "-p", choices=["WEEKLY", "MONTHLY", "PLUS3M", "YEARLY", "LIFETIME"], default="PLUS3M", help="Subscription Plan")
    lic_parser.add_argument("--days", type=int, default=90, help="Custom duration in days")

    # Promo subparser
    promo_parser = subparsers.add_parser("promo", help="Generate promotional voucher code")
    promo_parser.add_argument("--name", "-n", required=True, help="Promo name, e.g. PROMO5K, MERDEKA, TESTER")
    promo_parser.add_argument("--days", "-d", type=int, default=7, help="Free days granted")

    args = parser.parse_args()

    if args.command == "license":
        days = 7 if args.plan == "WEEKLY" else (90 if args.plan == "PLUS3M" else (365 if args.plan == "YEARLY" else args.days))
        key = generate_license(args.plan, args.device, days=days)
        print("\n" + "="*50)
        print(f"🔑 LISENSI ARBCN PRO BERHASIL DIGENERATE")
        print("="*50)
        print(f"Device ID : {args.device.upper()}")
        print(f"Paket     : {args.plan}")
        print(f"Durasi    : {'Selamanya' if args.plan == 'LIFETIME' else f'{days} Hari'}")
        print("-" * 50)
        print(f"KODE LISENSI:\n\n{key}\n")
        print("="*50)
        print("Kirimkan kode di atas kepada pelanggan via WhatsApp/Telegram.")
    elif args.command == "promo":
        promo = generate_promo(args.name, args.days)
        print("\n" + "="*50)
        print(f"🎟️ KODE PROMO ARBCN BERHASIL DIGENERATE")
        print("="*50)
        print(f"Nama Promo: {args.name.upper()}")
        print(f"Gratis    : {args.days} Hari")
        print("-" * 50)
        print(f"KODE VOUCHER:\n\n{promo}\n")
        print("="*50)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
