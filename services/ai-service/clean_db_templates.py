import psycopg2, re, sys
sys.stdout.reconfigure(encoding='utf-8')

conn = psycopg2.connect('postgresql://user:root@localhost:5432/notification')
cur = conn.cursor()

emoji_pattern = re.compile(r'[\U00010000-\U0010ffff]|[\u2000-\u32ff]|[\ufe00-\ufe0f]')

cur.execute('SELECT id, template_code, subject, body FROM notification_templates;')
rows = cur.fetchall()

for row_id, code, subject, body in rows:
    clean_sub = emoji_pattern.sub('', subject).strip()
    clean_sub = re.sub(r'\s+', ' ', clean_sub)
    clean_body = emoji_pattern.sub('', body)
    
    cur.execute('''
        UPDATE notification_templates 
        SET subject = %s, body = %s, updated_at = NOW()
        WHERE id = %s;
    ''', (clean_sub, clean_body, row_id))

conn.commit()

cur.execute('SELECT id, template_code, subject FROM notification_templates ORDER BY id;')
print("\n=== Verification ===")
for r in cur.fetchall():
    print(r[0], r[1], '-->', r[2])

conn.close()
