const fs = require('fs');
const path = require('path');

const outPath = path.resolve(__dirname, '../../output/pdf/petstore-app-summary.pdf');

function pdfEscape(text) {
  return text
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)');
}

function wrapText(text, maxChars) {
  const words = text.split(/\s+/);
  const lines = [];
  let current = '';
  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (candidate.length <= maxChars) {
      current = candidate;
    } else {
      if (current) lines.push(current);
      current = word;
    }
  }
  if (current) lines.push(current);
  return lines;
}

const ops = [];

function push(line) {
  ops.push(line);
}

function text(x, y, size, font, value, color = '0.11 0.14 0.19') {
  push('BT');
  push(`${color} rg`);
  push(`/${font} ${size} Tf`);
  push(`1 0 0 1 ${x} ${y} Tm`);
  push(`(${pdfEscape(value)}) Tj`);
  push('ET');
}

function multiline(x, y, size, font, lines, leading, color) {
  let cursor = y;
  for (const line of lines) {
    text(x, cursor, size, font, line, color);
    cursor -= leading;
  }
  return cursor;
}

function bulletList(x, y, widthChars, items, size = 9.4, leading = 11.5) {
  let cursor = y;
  for (const item of items) {
    const wrapped = wrapText(item, widthChars);
    text(x, cursor, size, 'F3', '-', '0.05 0.45 0.56');
    cursor = multiline(x + 10, cursor, size, 'F1', wrapped, leading, '0.11 0.14 0.19');
    cursor -= 2;
  }
  return cursor;
}

function numberedList(x, y, widthChars, items, size = 9.2, leading = 11.2) {
  let cursor = y;
  items.forEach((item, index) => {
    const wrapped = wrapText(item, widthChars);
    text(x, cursor, size, 'F3', `${index + 1}.`, '0.05 0.45 0.56');
    cursor = multiline(x + 14, cursor, size, 'F1', wrapped, leading, '0.11 0.14 0.19');
    cursor -= 2;
  });
  return cursor;
}

function heading(x, y, label) {
  text(x, y, 9.2, 'F3', label.toUpperCase(), '0.05 0.45 0.56');
}

function rect(x, y, w, h, fillRgb, strokeRgb = null, lineWidth = 1) {
  if (fillRgb) push(`${fillRgb} rg`);
  if (strokeRgb) push(`${strokeRgb} RG`);
  push(`${lineWidth} w`);
  push(`${x} ${y} ${w} ${h} re`);
  if (fillRgb && strokeRgb) push('B');
  else if (fillRgb) push('f');
  else push('S');
}

function line(x1, y1, x2, y2, rgb, width = 1) {
  push(`${rgb} RG`);
  push(`${width} w`);
  push(`${x1} ${y1} m`);
  push(`${x2} ${y2} l`);
  push('S');
}

const PAGE_W = 612;
const PAGE_H = 792;
const left = 40;
const right = 572;
const colGap = 22;
const colW = 255;
const col2 = left + colW + colGap;

rect(0, 0, PAGE_W, PAGE_H, '1 1 1');
line(left, 708, right, 708, '0.05 0.45 0.56', 2);

text(left, 738, 22, 'F2', 'Java PetStore Microservices');
multiline(
  left,
 719,
 9,
  'F1',
  wrapText(
    'Repo summary based on README, Maven modules, service configs, gateway routes, frontend code, and local Docker and Kubernetes assets.',
    80
  ),
  11,
  '0.38 0.41 0.45'
);

rect(360, 720, 212, 44, '0.90 0.97 0.98', '0.75 0.90 0.94', 1);
heading(372, 748, 'What It Is');
multiline(
  372,
  734,
  9.1,
  'F1',
  wrapText(
    'An educational PetStore app refactored into Spring Boot microservices with an Angular frontend, gateway, messaging, auth, and observability support.',
    42
  ),
  10.7,
  '0.11 0.14 0.19'
);

heading(left, 682, "Who It's For");
multiline(
  left,
  667,
  9.4,
  'F1',
  wrapText(
    'Primary persona: Java and full stack developers learning or demonstrating how an e-commerce style application can be split into Spring Boot microservices.',
    54
  ),
  11.4,
  '0.11 0.14 0.19'
);

heading(left, 626, 'What It Does');
bulletList(left, 611, 47, [
  'Shows products and lets users add new products through the Angular UI and product API.',
  'Accepts orders through the gateway and order service.',
  'Checks stock availability through inventory before saving an order.',
  'Publishes order-placed events to Kafka after successful orders.',
  'Consumes order events in notification-service and sends email confirmations.',
  'Exposes aggregated Swagger docs through the API gateway.',
  'Includes AI assistant chat endpoints in product-service via LangChain4j and OpenAI config.'
]);

heading(left, 333, 'How To Run');
numberedList(left, 318, 46, [
  'Start local infrastructure from repo root: docker compose up -d.',
  'Start the Angular app from frontend/: npm install, then npm start, and open http://localhost:4200.',
  'Start backend services: explicit repo-wide startup command not found; repo evidence shows Spring Boot apps in api-gateway, product-service, order-service, inventory-service, and notification-service.',
  'Optional for assistant chat: set OPENAI_API_KEY for product-service. Exact assistant usage docs: Not found in repo.'
]);

heading(col2, 682, 'How It Works');
rect(col2, 514, colW, 152, '0.985 0.99 0.995', '0.84 0.87 0.90', 1);
multiline(
  col2 + 12,
  646,
  9.2,
  'F3',
  ['Flow'],
  11,
  '0.11 0.14 0.19'
);
multiline(
  col2 + 12,
  633,
  9,
  'F1',
  wrapText(
    'Angular frontend calls the API gateway at localhost:9000 for product and order requests. Gateway routes /api/product, /api/order, and /api/inventory to backing services and applies circuit breaker fallbacks.',
    44
  ),
  10.8,
  '0.11 0.14 0.19'
);
multiline(
  col2 + 12,
  580,
  9.2,
  'F3',
  ['Data'],
  11,
  '0.11 0.14 0.19'
);
multiline(
  col2 + 12,
  567,
  9,
  'F1',
  wrapText(
    'Product-service uses MongoDB. Order-service and inventory-service use MySQL with Flyway. Order-service calls inventory synchronously, saves the order, then publishes a Kafka Avro event. Notification-service listens and sends email. Keycloak and OIDC config support auth; Grafana, Prometheus, Loki, and Tempo support observability.',
    44
  ),
  10.8,
  '0.11 0.14 0.19'
);

const servicesTop = 496;
const boxW = 121;
const boxH = 42;
const serviceBoxes = [
  [col2, servicesTop, 'Frontend', 'Angular 18 shop UI with OIDC client config.'],
  [col2 + boxW + 13, servicesTop, 'API Gateway', 'Spring Cloud Gateway MVC plus Swagger aggregation.'],
  [col2, servicesTop - 55, 'Product', 'REST plus MongoDB plus assistant chat endpoint.'],
  [col2 + boxW + 13, servicesTop - 55, 'Order', 'REST plus MySQL plus inventory client plus Kafka producer.'],
  [col2, servicesTop - 110, 'Inventory', 'REST plus MySQL stock checks.'],
  [col2 + boxW + 13, servicesTop - 110, 'Notification', 'Kafka consumer plus mail sender.']
];

for (const [x, y, title, body] of serviceBoxes) {
  rect(x, y, boxW, boxH, null, '0.84 0.87 0.90', 1);
  text(x + 8, y + 27, 8.8, 'F3', title, '0.11 0.14 0.19');
  multiline(x + 8, y + 15, 7.9, 'F1', wrapText(body, 23), 9, '0.38 0.41 0.45');
}

heading(col2, 325, 'Not Found In Repo');
bulletList(col2, 310, 43, [
  'A single canonical backend startup command or documented local boot order.',
  'End-user deployment guidance or a text-based production architecture diagram.'
], 9.2, 11);

line(left, 72, right, 72, '0.84 0.87 0.90', 1);
multiline(
  left,
  58,
  8.1,
  'F1',
  wrapText(
    'Evidence sources include README.md, docker-compose.yml, root and module pom.xml files, gateway routes, service application.properties, and Angular service and auth files.',
    100
  ),
  9.5,
  '0.38 0.41 0.45'
);

const stream = ops.join('\n');

const objects = [];

function addObject(body) {
  objects.push(body);
}

addObject('<< /Type /Catalog /Pages 2 0 R >>');
addObject('<< /Type /Pages /Kids [3 0 R] /Count 1 >>');
addObject('<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>');
addObject('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
addObject('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>');
addObject('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>');
addObject(`<< /Length ${Buffer.byteLength(stream, 'utf8')} >>\nstream\n${stream}\nendstream`);

let pdf = '%PDF-1.4\n';
const offsets = [0];

for (let i = 0; i < objects.length; i++) {
  offsets.push(Buffer.byteLength(pdf, 'utf8'));
  pdf += `${i + 1} 0 obj\n${objects[i]}\nendobj\n`;
}

const xrefStart = Buffer.byteLength(pdf, 'utf8');
pdf += `xref\n0 ${objects.length + 1}\n`;
pdf += '0000000000 65535 f \n';
for (let i = 1; i < offsets.length; i++) {
  pdf += `${String(offsets[i]).padStart(10, '0')} 00000 n \n`;
}
pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF`;

fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, pdf, 'binary');
console.log(outPath);
