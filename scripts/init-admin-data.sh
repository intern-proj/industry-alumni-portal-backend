#!/bin/bash
# ==============================================================================
# Shell wrapper to execute init-admin-data.py
# Seeds email templates, active SMTP server, and AI model presets in Azure
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/init-admin-data.py" "$@"
