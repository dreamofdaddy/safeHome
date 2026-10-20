from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from pypdf import PdfReader
import json

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "output" / "pdf"
OUT.mkdir(parents=True, exist_ok=True)

FONT = "Malgun"
FONT_BOLD = "MalgunBold"
pdfmetrics.registerFont(TTFont(FONT, r"C:\Windows\Fonts\malgun.ttf"))
pdfmetrics.registerFont(TTFont(FONT_BOLD, r"C:\Windows\Fonts\malgunbd.ttf"))

styles = getSampleStyleSheet()
normal = ParagraphStyle("Korean", parent=styles["BodyText"], fontName=FONT, fontSize=8.3, leading=13)
small = ParagraphStyle("Small", parent=normal, fontSize=7, leading=10, textColor=colors.HexColor("#4B5563"))
title = ParagraphStyle("TitleK", parent=styles["Title"], fontName=FONT_BOLD, fontSize=18, leading=24, alignment=TA_CENTER, textColor=colors.HexColor("#15261F"))
section = ParagraphStyle("Section", parent=normal, fontName=FONT_BOLD, fontSize=11, leading=15, textColor=colors.HexColor("#194D3E"))
warning = ParagraphStyle("Warning", parent=normal, fontName=FONT_BOLD, fontSize=8, leading=12, alignment=TA_CENTER, textColor=colors.HexColor("#A32727"))

def p(value, style=normal):
    return Paragraph(str(value).replace("\n", "<br/>"), style)

def watermark(canvas, doc):
    canvas.saveState()
    w, h = A4
    canvas.setFont(FONT_BOLD, 28)
    canvas.setFillColor(colors.Color(0.75, 0.1, 0.1, alpha=0.11))
    canvas.translate(w / 2, h / 2)
    canvas.rotate(35)
    canvas.drawCentredString(0, 0, "테스트 전용 · 법적 효력 없음")
    canvas.restoreState()
    canvas.saveState()
    canvas.setFont(FONT, 7)
    canvas.setFillColor(colors.HexColor("#6B7280"))
    canvas.drawString(18 * mm, 11 * mm, "SafeHome 합성 테스트 데이터 - 실제 인물·주소·권리관계가 아닙니다.")
    canvas.drawRightString(A4[0] - 18 * mm, 11 * mm, f"{doc.page}")
    canvas.restoreState()

def table(rows, widths, header=True):
    data = [[p(cell, small if i else normal) for i, cell in enumerate(row)] for row in rows]
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [
        ("FONTNAME", (0, 0), (-1, -1), FONT),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.45, colors.HexColor("#9CA3AF")),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    if header:
        commands += [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF4F0")),
            ("FONTNAME", (0, 0), (-1, 0), FONT_BOLD),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#194D3E")),
        ]
    for r in range(1 if header else 0, len(rows)):
        if r % 2 == 0:
            commands.append(("BACKGROUND", (0, r), (-1, r), colors.HexColor("#F8FAF9")))
    t.setStyle(TableStyle(commands))
    return t

def owner(name, masked, ratio="1.0", date="2021-03-12"):
    return {"name": name, "masked": masked, "ratio": ratio, "date": date}

def right(rank, purpose, received, detail, holder, amount, status="유효"):
    return {"rank": rank, "purpose": purpose, "received": received, "detail": detail, "holder": holder, "amount": amount, "status": status}

scenarios = [
    dict(code="01", slug="clean-single-owner", name="권리 제한 없는 단독 소유", number="1101-2026-000001", address="서울특별시 테스트구 안전로 101, 안전아파트 101동 501호", road="서울특별시 테스트구 안전로 101", building="안전아파트", unit="101동 501호", owners=[owner("김안전", "900101-*******")], rights=[], expected="판독 성공 / 낮은 위험 예상", transaction={"type":"JEONSE","deposit":120000000}),
    dict(code="02", slug="moderate-mortgage", name="단일 근저당", number="1101-2026-000002", address="서울특별시 테스트구 평온로 22, 평온빌라 202호", road="서울특별시 테스트구 평온로 22", building="평온빌라", unit="202호", owners=[owner("이평온", "850612-*******")], rights=[right("1", "근저당권설정", "2023-04-10", "채권최고액 금 120,000,000원", "테스트은행", "120000000")], expected="판독 성공 / 근저당 위험 요소 예상", transaction={"type":"JEONSE","deposit":80000000}),
    dict(code="03", slug="high-debt", name="복수 근저당과 높은 채권액", number="1101-2026-000003", address="부산광역시 테스트구 바다로 303, 파도오피스텔 1201호", road="부산광역시 테스트구 바다로 303", building="파도오피스텔", unit="1201호", owners=[owner("박파도", "780305-*******")], rights=[right("1", "근저당권설정", "2022-02-18", "채권최고액 금 260,000,000원", "가상은행", "260000000"), right("2", "근저당권설정", "2024-11-06", "채권최고액 금 90,000,000원", "안심저축은행", "90000000")], expected="판독 성공 / 높은 담보 위험 예상", transaction={"type":"JEONSE","deposit":250000000}),
    dict(code="04", slug="seizure", name="압류와 가압류", number="1101-2026-000004", address="대전광역시 테스트구 미래길 44, 미래주택 2층", road="대전광역시 테스트구 미래길 44", building="미래주택", unit="2층", owners=[owner("최미래", "920817-*******")], rights=[right("1", "압류", "2025-01-14", "조세 체납에 따른 압류", "테스트세무서", "", "유효"), right("2", "가압류", "2025-06-02", "청구금액 금 75,000,000원", "가상보증기금", "75000000", "유효")], expected="판독 성공 / 압류·가압류 고위험 예상", transaction={"type":"MONTHLY_RENT","deposit":30000000,"monthlyRent":900000}),
    dict(code="05", slug="co-owners", name="공동 소유", number="1101-2026-000005", address="인천광역시 테스트구 함께로 55, 한마음아파트 502동 703호", road="인천광역시 테스트구 함께로 55", building="한마음아파트", unit="502동 703호", owners=[owner("정하나", "880202-*******", "0.5"), owner("정두리", "900909-*******", "0.5")], rights=[], expected="판독 성공 / 공동소유 확인 요소 예상", transaction={"type":"SALE","salePrice":480000000}),
    dict(code="06", slug="cancelled-rights", name="말소된 근저당과 압류", number="1101-2026-000006", address="광주광역시 테스트구 새봄로 66, 새봄아파트 601호", road="광주광역시 테스트구 새봄로 66", building="새봄아파트", unit="601호", owners=[owner("한새봄", "810411-*******", "1.0", "2024-03-20")], rights=[right("1", "근저당권설정", "2020-08-17", "채권최고액 금 150,000,000원 / 2024-03-20 말소", "예전은행", "150000000", "말소"), right("2", "압류", "2021-09-03", "2022-01-10 말소", "테스트구청", "", "말소")], expected="판독 성공 / 말소 권리는 INACTIVE 처리", transaction={"type":"JEONSE","deposit":150000000}),
    dict(code="07", slug="leasehold", name="선순위 전세권", number="1101-2026-000007", address="경기도 테스트시 우선로 77, 우선빌딩 803호", road="경기도 테스트시 우선로 77", building="우선빌딩", unit="803호", owners=[owner("오우선", "760725-*******")], rights=[right("1", "전세권설정", "2023-07-01", "전세금 금 180,000,000원 / 존속기간 2027-06-30", "임차테스트", "180000000")], expected="판독 성공 / 선순위 권리 위험 예상", transaction={"type":"JEONSE","deposit":160000000}),
    dict(code="08", slug="complex-risk", name="공동소유·근저당·가압류 복합", number="1101-2026-000008", address="제주특별자치도 테스트시 복합로 88, 복합타운 3동 1004호", road="제주특별자치도 테스트시 복합로 88", building="복합타운", unit="3동 1004호", owners=[owner("강복합", "830101-*******", "0.6"), owner("윤복합", "840202-*******", "0.4")], rights=[right("1", "근저당권설정", "2022-05-11", "채권최고액 금 300,000,000원", "테스트은행", "300000000"), right("2", "가압류", "2025-09-15", "청구금액 금 130,000,000원", "가상채권자", "130000000")], expected="판독 성공 / 복합 고위험 예상", transaction={"type":"SALE","salePrice":520000000}),
    dict(code="09", slug="ownership-changes", name="소유권 이전 이력과 현재 소유자", number="1101-2026-000009", address="강원특별자치도 테스트시 이력길 99, 기록아파트 905호", road="강원특별자치도 테스트시 이력길 99", building="기록아파트", unit="905호", owners=[owner("남현재", "950505-*******", "1.0", "2025-12-01")], former=[owner("서과거", "700707-*******", "1.0", "2018-05-03")], rights=[], expected="판독 성공 / 말소된 이전 소유자를 제외하고 현재 소유자만 추출", transaction={"type":"JEONSE","deposit":100000000}),
    dict(code="10", slug="invalid-missing-required", name="필수 식별정보 누락", number="", address="테스트 주소 일부만 표시된 문서", road="", building="", unit="", owners=[], rights=[], expected="판독 실패 예상 / 필수 고유번호·현재 소유자 누락", transaction={"type":"JEONSE","deposit":50000000}),
]

def build(s):
    path = OUT / f"safehome-registry-{s['code']}-{s['slug']}.pdf"
    doc = SimpleDocTemplate(str(path), pagesize=A4, rightMargin=18*mm, leftMargin=18*mm, topMargin=16*mm, bottomMargin=19*mm, title=f"SafeHome 테스트 등기부 - {s['name']}", author="SafeHome Test Fixture Generator")
    story = [p("테스트 전용 합성 등기사항증명서", title), p("실제 발급 문서가 아니며 법적 효력이 없습니다. 테스트 시스템 외 사용을 금합니다.", warning), Spacer(1, 6*mm)]
    story += [table([
        ["테스트 시나리오", s["name"]], ["부동산 고유번호", s["number"] or "(의도적으로 누락)"], ["열람 일시", "2026-10-14 14:30:00"], ["문서 구분", "집합건물 / 테스트 데이터"],
    ], [42*mm, 132*mm], header=False), Spacer(1, 5*mm)]
    story += [p("[표제부] 부동산의 표시", section), Spacer(1, 2*mm), table([
        ["표시번호", "접수", "소재지번·건물명칭", "건물내역"],
        ["1", "2021-03-12", s["address"], f"도로명: {s['road'] or '(누락)'}\n건물명: {s['building'] or '(누락)'}\n호수: {s['unit'] or '(누락)'}"],
    ], [16*mm, 25*mm, 78*mm, 55*mm]), Spacer(1, 5*mm)]
    story += [p("[갑구] 소유권에 관한 사항", section), Spacer(1, 2*mm)]
    owner_rows = [["순위", "등기목적", "접수", "권리자 및 기타사항", "상태"]]
    for i, o in enumerate(s.get("former", []), 1):
        owner_rows.append([str(i), "소유권이전", o["date"], f"소유자 {o['name']} {o['masked']}\n지분 {o['ratio']}", "말소"])
    offset = len(s.get("former", []))
    for i, o in enumerate(s["owners"], 1):
        owner_rows.append([str(i+offset), "소유권이전", o["date"], f"현재 소유자 {o['name']} {o['masked']}\n지분 {o['ratio']}", "유효"])
    if not s["owners"]:
        owner_rows.append(["-", "확인 불가", "-", "현재 소유자 정보가 확인되지 않음", "오류"])
    story += [table(owner_rows, [14*mm, 27*mm, 24*mm, 88*mm, 21*mm]), Spacer(1, 5*mm)]
    story += [p("[을구 및 관련 갑구] 소유권 이외의 권리에 관한 사항", section), Spacer(1, 2*mm)]
    right_rows = [["순위", "등기목적", "접수", "권리내용", "권리자", "상태"]]
    for r in s["rights"]:
        right_rows.append([r["rank"], r["purpose"], r["received"], r["detail"], r["holder"], r["status"]])
    if not s["rights"]:
        right_rows.append(["-", "기록사항 없음", "-", "현재 유효한 제한물권 기록 없음", "-", "-"])
    story += [table(right_rows, [12*mm, 27*mm, 23*mm, 68*mm, 29*mm, 15*mm]), Spacer(1, 6*mm)]
    story += [p("테스트 판독 참고", section), Spacer(1, 2*mm), table([
        ["예상 결과", s["expected"]], ["Frontend 등록 주소", s["address"]], ["주의", "합성 데이터입니다. 실제 계약·법률 판단에 사용할 수 없습니다."],
    ], [42*mm, 132*mm], header=False)]
    doc.build(story, onFirstPage=watermark, onLaterPages=watermark)
    reader = PdfReader(str(path))
    if len(reader.pages) < 1 or not (reader.metadata.title or "").startswith("SafeHome"):
        raise RuntimeError(f"invalid output: {path}")
    return path

created = [build(s) for s in scenarios]
(OUT / "scenarios.json").write_text(json.dumps(scenarios, ensure_ascii=False, indent=2), encoding="utf-8")
print("\n".join(str(p) for p in created))
