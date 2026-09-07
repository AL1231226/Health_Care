#!/bin/bash
BASE=http://localhost:8082
echo "== 1 无token(未登录拦截) =="
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -d '{"messages":[{"role":"user","content":"hi"}]}'
echo; echo "== 2 user2 正常请求(无key应优雅提示) =="
UT=$(curl -s -X POST $BASE/auth/family/login -H "Content-Type: application/json" -d '{"phone":"17749477385","password":"123456ag","role":1}' | python -c "import json,sys;print(json.load(sys.stdin)['data']['token'])")
printf '%s' '{"messages":[{"role":"user","content":"你们平台有什么助餐服务"}]}' > b2.json
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -H "Authorization: Bearer $UT" --data-binary @b2.json
echo; echo "== 3 商家token越权(应无权限) =="
PT=$(curl -s -X POST $BASE/auth/provider/login -H "Content-Type: application/json" -d '{"phone":"13811001234","password":"123456a","role":3}' | python -c "import json,sys;print(json.load(sys.stdin)['data']['token'])")
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -H "Authorization: Bearer $PT" -d '{"messages":[{"role":"user","content":"hi"}]}'
echo; echo "== 4 空messages(应拒) =="
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -H "Authorization: Bearer $UT" -d '{"messages":[]}'
echo; echo "== 5 末条非user(应拒) =="
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -H "Authorization: Bearer $UT" -d '{"messages":[{"role":"assistant","content":"hi"}]}'
echo; echo "== 6 携带历史多轮(无key同样优雅提示,验证截留不炸) =="
curl -s -X POST $BASE/ai/chat -H "Content-Type: application/json" -H "Authorization: Bearer $UT" -d '{"messages":[{"role":"user","content":"a"},{"role":"assistant","content":"b"},{"role":"user","content":"c"}]}'
echo
