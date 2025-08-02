import firebase_admin
from firebase_admin import credentials, auth

cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)

# 需要设置权限的用户 UID
uid = ''

# 设置自定义声明
auth.set_custom_user_claims(uid, {'isAdmin' :True})

print('成功设置管理员权限')
