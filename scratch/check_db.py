import psycopg2

conn = psycopg2.connect(
    host='nicdbpgs.postgres.database.azure.com',
    port=5432,
    dbname='user_db',
    user='pguser',
    password='NicDB@123',
    sslmode='require'
)
cur = conn.cursor()

# 1. Check columns in user_profiles
cur.execute("""
    SELECT column_name, data_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_profiles'
    ORDER BY ordinal_position;
""")
cols = cur.fetchall()
print('=== COLUMNS IN user_profiles ===')
for col in cols:
    print(f'  {col[0]} ({col[1]})')

# 2. Check student1 in user_profiles
cur.execute("SELECT * FROM user_profiles WHERE user_id = 'student1';")
row = cur.fetchone()
print('\n=== ROW FOR student1 in user_profiles ===')
if row:
    colnames = [desc[0] for desc in cur.description]
    for k, v in zip(colnames, row):
        print(f'  {k}: {v}')
else:
    print('  student1 not found in user_profiles')

# 3. Check all student usernames in user_profiles
cur.execute("SELECT user_id, first_name, last_name, email FROM user_profiles ORDER BY user_id LIMIT 15;")
students = cur.fetchall()
print('\n=== ALL PROFILES IN user_profiles ===')
for s in students:
    print(f'  {s}')

# 4. Check skills for student1
cur.execute("SELECT skill_id, skill_name, skill_level FROM skills WHERE user_id = 'student1';")
skills = cur.fetchall()
print('\n=== SKILLS FOR student1 ===')
for s in skills:
    print(f'  {s}')

# 6. Check columns in academic_records
cur.execute("""
    SELECT column_name, data_type 
    FROM information_schema.columns 
    WHERE table_name = 'academic_records'
    ORDER BY ordinal_position;
""")
acols = cur.fetchall()
print('\n=== COLUMNS IN academic_records ===')
for col in acols:
    print(f'  {col[0]} ({col[1]})')

# 7. Check flyway_schema_history
try:
    cur.execute("SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;")
    print('\n=== FLYWAY SCHEMA HISTORY ===')
    for f in cur.fetchall():
        print(f'  {f}')
except Exception as e:
    print(f'  Flyway history error: {e}')

cur.close()
conn.close()

