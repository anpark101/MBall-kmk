#!/usr/bin/env python3
"""Probe MangaBall API through several routes to find one that works from CI."""
import json
import sys
import urllib.parse
import urllib.request

API = "https://mangaball.com/api/v1/title/search-advanced?page=1&limit=1"
UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"


def fetch(url, headers=None, timeout=60):
    req = urllib.request.Request(url, headers=headers or {"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.status, resp.read().decode("utf-8", "replace")


def report(name, fn):
    try:
        status, body = fn()
        print(f"### {name} -> HTTP {status} len={len(body)}")
        print(body[:400].replace("\n", " "))
    except Exception as exc:  # noqa: BLE001
        print(f"### {name} -> ERROR {type(exc).__name__}: {str(exc)[:250]}")
    print("---")
    sys.stdout.flush()


def main():
    quoted = urllib.parse.quote(API, safe="")
    routes = {
        "direct": lambda: fetch(API, {"User-Agent": UA, "Accept": "application/json"}),
        "jina": lambda: fetch("https://r.jina.ai/" + API, {"User-Agent": UA}),
        "codetabs": lambda: fetch(f"https://api.codetabs.com/v1/proxy?quest={quoted}"),
        "allorigins": lambda: fetch(f"https://api.allorigins.win/raw?url={quoted}"),
        "corsproxy": lambda: fetch(f"https://corsproxy.io/?url={quoted}"),
        "thingproxy": lambda: fetch(f"https://thingproxy.freeboard.io/fetch/{API}"),
        "textance": lambda: fetch(f"https://api.scraperapi.com/?url={quoted}"),
        "proxy-cors": lambda: fetch(f"https://proxy.cors.sh/{API}"),
    }
    for name, fn in routes.items():
        report(name, fn)

    # curl_cffi based attempts
    try:
        from curl_cffi import requests as cffi_requests

        for imp in ("chrome", "chrome124", "safari17_0"):
            def attempt(imp=imp):
                r = cffi_requests.get(
                    API,
                    impersonate=imp,
                    timeout=45,
                    headers={
                        "Accept": "application/json, text/plain, */*",
                        "Referer": "https://mangaball.com/search-advanced",
                    },
                )
                return r.status_code, r.text

            report(f"curl_cffi:{imp}", attempt)
    except Exception as exc:  # noqa: BLE001
        print(f"### curl_cffi unavailable -> {type(exc).__name__}: {exc}")

    # JSON sanity check on whatever came back
    print("## JSON sanity")
    try:
        status, body = fetch(API, {"User-Agent": UA, "Accept": "application/json"})
        data = json.loads(body)
        print("parsed keys:", list(data.keys())[:10], "first title:", data["data"][0]["name"])
    except Exception as exc:  # noqa: BLE001
        print("parse failed:", type(exc).__name__, str(exc)[:200])


if __name__ == "__main__":
    main()
