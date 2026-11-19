export const FIREBASE_CONFIG = {
  projectId: "gen-lang-client-0901479759",
  databaseId: "ai-studio-android-supplych-513b51f6-bde3-4175-87ec-0f32026fdb7f",
  authDomain: "gen-lang-client-0901479759.firebaseapp.com",
  appId: "com.aistudio.supplychain.scm",
  versionJsonUrl: "https://gen-lang-client-0901479759.web.app/version.json",
  apkDownloadUrl: "https://gen-lang-client-0901479759.web.app/downloads/ISCEB-latest.apk"
};

export const AVAILABLE_USERS = [
  { id: "student-1", fullName: "Sarah Jenkins", email: "sarah.jenkins@isceb.org", role: "STUDENT", organization: "ISCEB PGD Candidate", designation: "PGD Candidate" },
  { id: "student-2", fullName: "Alex Rivera", email: "alex.rivera@freightcorp.com", role: "STUDENT", organization: "Pacific Maritime Forwarding", designation: "PGD Candidate" },
  { id: "student-3", fullName: "Tanvir Ahmed", email: "tanvir.ahmed@dhakalogistics.com", role: "STUDENT", organization: "Apex SCM Bangladesh", designation: "PGD Candidate" },
  { id: "admin-2", fullName: "Md. Amirul Islam", email: "amirul.islam@isceb.org", role: "ADMIN", organization: "Head of Supply Chain Management at BRB Group", designation: "Founder of ISCEB", additionalRole: "Lead Trainer of ISCEB", mobileNumber: "01715-282905" },
  { id: "admin-1", fullName: "Md. Nafiul Islam Nahin", email: "www.mdnafiulislam@gmail.com", role: "ADMIN", organization: "ISCEB App Engineering", designation: "Developer of ISCEB App" }
];

export const DEFAULT_FOUNDER = {
  name: "Md. Amirul Islam",
  designation: "Founder of ISCEB",
  professionalIdentity: "Head of Supply Chain Management at BRB Group",
  additionalRole: "Lead Trainer of ISCEB",
  currentOrganization: "BRB Group",
  mobileNumber: "01715-282905",
  email: "amirul.islam@isceb.org",
  location: "Dhaka, Bangladesh",
  biography: "Founder of ISCEB (International Supply Chain Education Board) and Lead Trainer of ISCEB, serving as Head of Supply Chain Management at BRB Group with executive leadership in strategic procurement, global logistics, and SCM mentorship."
};

export const DEFAULT_DEVELOPER = {
  name: "Md. Nafiul Islam Nahin",
  designation: "Developer of ISCEB App",
  biography: "App Developer for ISCEB (International Supply Chain Education Board), architecting Android, Web, and PC/Desktop platforms with real-time cloud synchronization, encrypted assessments, and verified executive diploma issuance.",
  contactEmail: "www.mdnafiulislam@gmail.com",
  location: "Dhaka, Bangladesh"
};

export const DEFAULT_MEMBERS = [
  {
    id: "member-founder-amirul",
    fullName: "Md. Amirul Islam",
    designation: "Founder of ISCEB",
    professionalIdentity: "Head of Supply Chain Management at BRB Group",
    currentOrganization: "BRB Group",
    roleInIsceb: "Founder of ISCEB • Lead Trainer of ISCEB",
    contactInformation: "Mobile: 01715-282905 • Email: amirul.islam@isceb.org",
    biography: "Founder of ISCEB and Lead Trainer guiding supply chain management professionals while serving as Head of Supply Chain Management at BRB Group.",
    displayOrder: 1
  }
];

export const INITIAL_MODULES = [
  { id: "mod-1", number: 1, title: "Concept of SCM and Demand Forecasting & Planning", description: "Orientation, core definitions, cross-functional flow, demand planning/forecasting principles, and Module 01 Exam (20 Marks).", part: "PART_1_BASIC", category: "Foundation", estimatedHours: 5, isRequired: true },
  { id: "mod-2", number: 2, title: "Sourcing & Vendor Management", description: "Strategic sourcing, negotiations, vendor management & selection, enlistment process, and Assignment & Exam (25 Marks).", part: "PART_1_BASIC", category: "Sourcing", estimatedHours: 5, isRequired: true },
  { id: "mod-3", number: 3, title: "Procurement (Local & International)", description: "Local procurement, PO/WO, purchase register & bill checklist, complete import workflow, LC types, PI/Incoterms/HS Code, export process, and Assignment & Exam (30 Marks).", part: "PART_1_BASIC", category: "Procurement", estimatedHours: 9, isRequired: true },
  { id: "mod-4", number: 4, title: "Inventory, Warehouse, Distribution & Logistics", description: "Inventory management systems, warehouse management systems, manufacturing & production, delivery & distribution, and transportation & logistics.", part: "PART_1_BASIC", category: "Operations", estimatedHours: 5, isRequired: true },
  { id: "mod-5", number: 5, title: "Billing Management & CRM", description: "Bill process, bond & VAT-Tax compliance, and Customer Relationship Management (CRM) in supply chains.", part: "PART_1_BASIC", category: "Finance & CRM", estimatedHours: 2, isRequired: true },
  { id: "mod-6", number: 6, title: "SOP & QMS", description: "Standard Operating Procedures (SOP), Quality Management Systems (QMS), and Module 06 Exam (25 Marks).", part: "PART_1_BASIC", category: "Quality & Standards", estimatedHours: 2, isRequired: true },
  { id: "mod-7", number: 7, title: "Introduction to Supply Chain Management", description: "Strategic SCM overview, integration & global trends, SCM vs Logistics, and 3PL/4PL, reverse logistics & last-mile delivery.", part: "PART_2_ADVANCE", category: "Strategy", estimatedHours: 3, isRequired: true },
  { id: "mod-8", number: 8, title: "Procurement & Sourcing Management", description: "Procurement policies, strategic sourcing, digital procurement, supplier evaluation, make-or-buy decisions, and cost management.", part: "PART_2_ADVANCE", category: "Advanced Sourcing", estimatedHours: 3, isRequired: true },
  { id: "mod-9", number: 9, title: "Inventory, Warehouse & Operations Management", description: "Inventory fundamentals, control models, warehouse design & space optimization, production/capacity planning, Lean, JIT & TQM.", part: "PART_2_ADVANCE", category: "Advanced Operations", estimatedHours: 3, isRequired: true },
  { id: "mod-10", number: 10, title: "International Trade & Supply Chain Finance", description: "Letter of Credit (L/C), banking procedures, Incoterms, customs clearance, freight forwarder selection & landed cost, case study, and working capital.", part: "PART_2_ADVANCE", category: "Global Finance", estimatedHours: 6, isRequired: true },
  { id: "mod-11", number: 11, title: "Supply Chain Planning, Strategy & Technology", description: "Demand planning, MRP & DRP systems, and digital supply chain integration across ERP, SAP, IoT, AI, Big Data, Analytics & Blockchain.", part: "PART_2_ADVANCE", category: "Digital Tech & ERP", estimatedHours: 2, isRequired: true },
  { id: "mod-12", number: 12, title: "Sustainable Supply Chain & Project Work", description: "Waste reduction, Green SCM, CSR, Green Procurement & Eco-Design, Group Discussion & Course Review, and Final Exam (100 Marks).", part: "PART_2_ADVANCE", category: "Capstone & Sustainability", estimatedHours: 3, isRequired: true }
];

export const INITIAL_CLASSES = [
  { id: "class-1", moduleId: "mod-1", orderIndex: 1, lectureCode: "01", title: "Orientation and Concept of SCM", summary: "Course roadmap, ISCEB standards, defining modern supply chains, and three fundamental flows (material, information, financial).", durationMinutes: 30, isPublished: true, learningObjectives: ["ISCEB PGD course expectations", "Definitions and scope of SCM", "3 foundational flows"] },
  { id: "class-2", moduleId: "mod-1", orderIndex: 2, lectureCode: "02", title: "Basic of SCM", summary: "The 5 pillars: Plan, Source, Make, Deliver, Return. Functional silos vs integrated end-to-end supply chains.", durationMinutes: 30, isPublished: true, learningObjectives: ["SCOR framework overview", "Breaking down functional silos", "Supply chain drivers"] },
  { id: "class-3", moduleId: "mod-1", orderIndex: 3, lectureCode: "03", title: "Demand Forecasting & Planning - Basic", summary: "Foundational principles of demand planning, qualitative vs quantitative forecasting, and seasonal demand factors.", durationMinutes: 30, isPublished: true, learningObjectives: ["Principles of demand forecasting", "Moving average & exponential smoothing", "Managing forecast volatility"] },
  { id: "class-4", moduleId: "mod-1", orderIndex: 4, lectureCode: "04", title: "Demand Forecasting & Planning - PR, SR & Forecasting", summary: "Translating forecasts into Purchase Requisition (PR), Store Requisition (SR), and master planning schedules.", durationMinutes: 30, isPublished: true, learningObjectives: ["Purchase Requisition generation", "Store Requisition workflows", "Demand aggregation"] },
  { id: "class-5", moduleId: "mod-1", orderIndex: 5, lectureCode: "05", title: "Exam (20 Marks)", summary: "Module 01 formal assessment evaluating Concept of SCM and Demand Forecasting & Planning (20 Marks).", durationMinutes: 30, isPublished: true, learningObjectives: ["SCM concept mastery check", "Demand planning application", "PR & SR operational assessment"] },
  { id: "class-6", moduleId: "mod-2", orderIndex: 1, lectureCode: "06", title: "Sourcing", summary: "Strategic sourcing methodology, supplier discovery, market benchmarking, and total cost of ownership (TCO).", durationMinutes: 30, isPublished: true, learningObjectives: ["7-step sourcing process", "Supplier market research", "TCO calculations"] },
  { id: "class-7", moduleId: "mod-2", orderIndex: 2, lectureCode: "07", title: "Negotiations", summary: "Principles of commercial negotiation, BATNA preparation, bargaining zones, and win-win supplier partnerships.", durationMinutes: 30, isPublished: true, learningObjectives: ["BATNA preparation", "Negotiation strategies in procurement", "Contract closure techniques"] },
  { id: "class-8", moduleId: "mod-2", orderIndex: 3, lectureCode: "08", title: "Vendor Management & Selection", summary: "Vendor lifecycle management, scorecards, selection matrices, relationship tiers, and SLA monitoring mechanisms.", durationMinutes: 30, isPublished: true, learningObjectives: ["Supplier KPI scorecards", "Vendor selection criteria", "Mitigating supplier concentration risks"] },
  { id: "class-9", moduleId: "mod-2", orderIndex: 4, lectureCode: "09", title: "Vendor Enlistment Process", summary: "Standard vendor enlistment documentation, legal compliance checks, financial vetting, and audit trails.", durationMinutes: 30, isPublished: true, learningObjectives: ["Enlistment document verification", "Due diligence and statutory compliance", "Vendor audit workflows"] },
  { id: "class-10", moduleId: "mod-2", orderIndex: 5, lectureCode: "10", title: "Assignment & Exam (25 Marks)", summary: "Module 02 practical assignment and formal examination on Sourcing & Vendor Management (25 Marks).", durationMinutes: 40, isPublished: true, learningObjectives: ["Strategic sourcing application", "Vendor scorecard & negotiation evaluation", "Module 02 exam readiness"] },
  { id: "class-11", moduleId: "mod-3", orderIndex: 1, lectureCode: "11", title: "Basic of Procurement", summary: "Commercial procurement cycles, requisition review, compliance checks, and authorization limits.", durationMinutes: 30, isPublished: true, learningObjectives: ["Procure-to-Pay (P2P) cycle", "Procurement ethics", "Authorization matrices"] },
  { id: "class-12", moduleId: "mod-3", orderIndex: 2, lectureCode: "12", title: "Local Procurement - PO, WO, Purchase Register & Bill Check List", summary: "Drafting Purchase Orders (PO) and Work Orders (WO), maintaining compliant Purchase Registers, and executing Bill Check Lists.", durationMinutes: 30, isPublished: true, learningObjectives: ["Drafting legally binding POs/WOs", "Purchase register maintenance", "Bill check list & 3-way matching"] },
  { id: "class-13", moduleId: "mod-3", orderIndex: 3, lectureCode: "13", title: "Import - Basic", summary: "Fundamentals of international purchasing, regulatory frameworks, foreign exchange regulations, and country import policies.", durationMinutes: 30, isPublished: true, learningObjectives: ["Import policy orders", "Foreign exchange regulations", "Import permits & IRC requirements"] },
  { id: "class-14", moduleId: "mod-3", orderIndex: 4, lectureCode: "14", title: "Import - Flow Chart", summary: "End-to-end import visual flow chart from indenting and opening LC to port discharge and final warehouse delivery.", durationMinutes: 30, isPublished: true, learningObjectives: ["Visualizing import milestones", "Banking & customs coordination", "Lead time buffer control"] },
  { id: "class-15", moduleId: "mod-3", orderIndex: 5, lectureCode: "15", title: "Import - LC Types", summary: "Comprehensive breakdown of Letters of Credit: Sight, Usance/Deferred, Irrevocable, Back-to-Back, Revolving, and Standby LCs.", durationMinutes: 30, isPublished: true, learningObjectives: ["UCP 600 rules", "Irrevocable Sight vs Usance LCs", "Back-to-Back LC mechanisms"] },
  { id: "class-16", moduleId: "mod-3", orderIndex: 6, lectureCode: "16", title: "Import - Proforma Invoice, Incoterms & H.S Code", summary: "Scrutiny of Proforma Invoices (PI), Incoterms 2020 obligations (FOB, CFR, CIF, DDP), and Harmonized System (HS) code classification.", durationMinutes: 30, isPublished: true, learningObjectives: ["Incoterms 2020 risk transfer points", "HS Code tariff determination", "PI clause validation"] },
  { id: "class-17", moduleId: "mod-3", orderIndex: 7, lectureCode: "17", title: "Import - All Documents", summary: "Detailed analysis of Commercial Invoice, Packing List, Bill of Lading (B/L) / Air Waybill (AWB), Certificate of Origin, and Inspection Certificates.", durationMinutes: 30, isPublished: true, learningObjectives: ["B/L vs AWB legal distinctions", "Certificate of Origin rules", "Discrepancy prevention"] },
  { id: "class-18", moduleId: "mod-3", orderIndex: 8, lectureCode: "18", title: "Export Process", summary: "Export processing workflows, EXP issuance, customs bonded warehouse licensing, export documentation, and duty drawback facilities.", durationMinutes: 30, isPublished: true, learningObjectives: ["Export documentation workflow", "Bonded warehouse facilities", "Duty drawback utilization"] },
  { id: "class-19", moduleId: "mod-3", orderIndex: 9, lectureCode: "19", title: "Assignment & Exam (30 Marks)", summary: "Module 03 practical assignment and comprehensive examination on Local & International Procurement (30 Marks).", durationMinutes: 45, isPublished: true, learningObjectives: ["Local PO/WO & import compliance check", "LC & Incoterms case assessment", "Export-import synthesis"] },
  { id: "class-20", moduleId: "mod-4", orderIndex: 1, lectureCode: "20", title: "Inventory Management System", summary: "Inventory ledger maintenance, stock valuation (FIFO, LIFO, Weighted Average), and physical stock reconciliation.", durationMinutes: 30, isPublished: true, learningObjectives: ["Inventory valuation models", "Cycle counting vs annual audit", "Shrinkage management"] },
  { id: "class-21", moduleId: "mod-4", orderIndex: 2, lectureCode: "21", title: "Warehouse Management System", summary: "WMS architecture, warehouse zones, material handling equipment, slotting strategies, and barcode/RFID tracking.", durationMinutes: 30, isPublished: true, learningObjectives: ["Warehouse zone optimization", "Cross-docking and slotting", "WMS software architecture"] },
  { id: "class-22", moduleId: "mod-4", orderIndex: 3, lectureCode: "22", title: "Manufacturing & Production", summary: "Production scheduling, bill of materials (BOM), work-in-progress (WIP) tracking, and capacity bottleneck management.", durationMinutes: 30, isPublished: true, learningObjectives: ["Bill of Materials structures", "WIP tracking", "Capacity requirement planning"] },
  { id: "class-23", moduleId: "mod-4", orderIndex: 4, lectureCode: "23", title: "Delivery & Distribution", summary: "Fulfillment workflows, order picking systems (wave, batch, zone), staging, dispatch, and proof-of-delivery (POD).", durationMinutes: 30, isPublished: true, learningObjectives: ["Picking strategies", "Order staging and dispatch", "Electronic POD (e-POD)"] },
  { id: "class-24", moduleId: "mod-4", orderIndex: 5, lectureCode: "24", title: "Transportation & Logistics", summary: "Freight modal selection (road, rail, ocean, air), carrier contracts, freight consolidation, and container load optimization.", durationMinutes: 30, isPublished: true, learningObjectives: ["Modal economics and transit times", "FCL vs LCL consolidation", "Fleet routing tactics"] },
  { id: "class-25", moduleId: "mod-5", orderIndex: 1, lectureCode: "25", title: "Bill Process, Bond & VAT-Tax", summary: "Supplier bill processing, customs bond reconciliation, statutory VAT deduction at source (VDS), withholding tax regulations, and audit compliance.", durationMinutes: 30, isPublished: true, learningObjectives: ["Bill verification procedures", "Bond reconciliation & VDS", "Tax compliance in procurement"] },
  { id: "class-26", moduleId: "mod-5", orderIndex: 2, lectureCode: "26", title: "Customer Relationship Management", summary: "Customer order fulfillment cycles, On-Time In-Full (OTIF) metrics, customer dispute resolution, and retention strategies.", durationMinutes: 30, isPublished: true, learningObjectives: ["OTIF calculation", "Customer service level agreements", "Collaborative forecasting"] },
  { id: "class-27", moduleId: "mod-6", orderIndex: 1, lectureCode: "27", title: "SOP & QMS", summary: "Designing bulletproof Standard Operating Procedures (SOP) for logistics, ISO 9001 Quality Management System architecture, and continuous improvement (Kaizen/PDCA).", durationMinutes: 30, isPublished: true, learningObjectives: ["Standard Operating Procedure drafting", "ISO 9001 QMS integration", "Root-cause analysis (5 Whys)"] },
  { id: "class-28", moduleId: "mod-6", orderIndex: 2, lectureCode: "28", title: "Exam (25 Marks)", summary: "Module 06 formal examination covering SOP & QMS operational governance (25 Marks).", durationMinutes: 35, isPublished: true, learningObjectives: ["SOP & QMS mastery verification", "ISO 9001 compliance checks", "Continuous improvement assessment"] },
  { id: "class-29", moduleId: "mod-7", orderIndex: 1, lectureCode: "29", title: "SCM - Overview, Key Concepts, Components, Integration & Global Trends", summary: "Strategic SCM overview, key components, upstream-downstream integration, geopolitical disruption responses, and global trends.", durationMinutes: 30, isPublished: true, learningObjectives: ["Evolution of supply chain paradigms", "Enterprise supply chain integration", "Global trends & resilience"] },
  { id: "class-30", moduleId: "mod-7", orderIndex: 2, lectureCode: "30", title: "SCM vs Logistics: Scope, Transportation & Distribution Systems", summary: "Differentiating SCM strategy from tactical logistics, distribution network modeling, and hub-and-spoke vs direct delivery.", durationMinutes: 30, isPublished: true, learningObjectives: ["Logistics vs SCM conceptual boundaries", "Hub-and-spoke distribution networks", "Total logistics cost curve"] },
  { id: "class-31", moduleId: "mod-7", orderIndex: 3, lectureCode: "31", title: "3PL/4PL, Reverse Logistics & Last-Mile Delivery", summary: "Contract logistics (3PL/4PL) governance, circular reverse logistics for returns, and urban last-mile delivery economics.", durationMinutes: 30, isPublished: true, learningObjectives: ["Selecting 3PL vs 4PL providers", "Reverse logistics flow optimization", "Last-mile unit economics"] },
  { id: "class-32", moduleId: "mod-8", orderIndex: 1, lectureCode: "32", title: "Procurement Management, Policies, Strategic Sourcing & Digital Procurement", summary: "Corporate procurement policy design, category management strategies, e-procurement reverse auctions, and spend analysis frameworks.", durationMinutes: 30, isPublished: true, learningObjectives: ["Category management methodology", "Digital e-procurement workflows", "Procurement governance models"] },
  { id: "class-33", moduleId: "mod-8", orderIndex: 2, lectureCode: "33", title: "Supplier Evaluation, Vendor Selection & Make-or-Buy Decisions", summary: "Kraljic portfolio matrix, multi-criteria vendor decision models (AHP), and quantitative make-or-buy financial models.", durationMinutes: 30, isPublished: true, learningObjectives: ["Kraljic portfolio matrix", "Analytical Hierarchy Process (AHP)", "Make-or-buy cost models"] },
  { id: "class-34", moduleId: "mod-8", orderIndex: 3, lectureCode: "34", title: "Supplier Relationship & Cost Management", summary: "Measuring procurement ROI, supplier development programs, target costing, and value engineering (VAVE).", durationMinutes: 30, isPublished: true, learningObjectives: ["Procurement ROI & savings metrics", "Value Analysis / Value Engineering", "Supplier development alliances"] },
  { id: "class-35", moduleId: "mod-9", orderIndex: 1, lectureCode: "35", title: "Inventory Fundamentals, Classification & Control Models", summary: "Economic Order Quantity (EOQ), ABC-FSN-VED classification, statistical safety stock, and Re-Order Point (ROP) optimization.", durationMinutes: 30, isPublished: true, learningObjectives: ["EOQ with quantity discounts", "Safety stock & ROP formulas", "Multi-criteria ABC/VED matrix"] },
  { id: "class-36", moduleId: "mod-9", orderIndex: 2, lectureCode: "36", title: "Warehouse Design, Layout Planning & Space Optimization", summary: "Warehouse capacity planning, aisle width calculation, racking systems (VNA, drive-in), and heat map layout modeling.", durationMinutes: 30, isPublished: true, learningObjectives: ["Warehouse cubic volume utilization", "Racking configuration trade-offs", "Picking travel time reduction"] },
  { id: "class-37", moduleId: "mod-9", orderIndex: 3, lectureCode: "37", title: "Production Planning, Capacity Planning, Lean, JIT & TQM", summary: "Master Production Scheduling (MPS), Rough-Cut Capacity Planning (RCCP), Kanban pull systems, and Total Quality Management.", durationMinutes: 30, isPublished: true, learningObjectives: ["MPS and RCCP coordination", "Kanban pull mechanism implementation", "Lean waste elimination (Muda)"] },
  { id: "class-38", moduleId: "mod-10", orderIndex: 1, lectureCode: "38", title: "Letter of Credit (L/C), Banking Procedures & Trade Documentation", summary: "Detailed UCP 600 articles, ISBP 745 documentation standards, discrepancy resolution, and trade factoring.", durationMinutes: 30, isPublished: true, learningObjectives: ["UCP 600 key articles & ISBP 745", "Bank confirmation & negotiation", "Preventing documentary rejections"] },
  { id: "class-39", moduleId: "mod-10", orderIndex: 2, lectureCode: "39", title: "Incoterms, Global Trade Rules & Export-Import", summary: "Advanced Incoterms 2020 legal nuances, marine insurance obligations, customs jurisdiction, and multilateral trade treaties.", durationMinutes: 30, isPublished: true, learningObjectives: ["Incoterms 2020 legal liabilities", "Marine insurance ICC (A/B/C)", "Trade compliance audits"] },
  { id: "class-40", moduleId: "mod-10", orderIndex: 3, lectureCode: "40", title: "Customs Clearance, Port Operations & Shipping Management", summary: "Customs assessment notice, Bill of Entry (B/E) filing, port demurrage & detention avoidance, and shipping agent roles.", durationMinutes: 30, isPublished: true, learningObjectives: ["Bill of Entry submission process", "Demurrage vs detention avoidance", "C&F agent management"] },
  { id: "class-41", moduleId: "mod-10", orderIndex: 4, lectureCode: "41", title: "Freight Forwarder Selection & Landed Cost", summary: "Evaluating international freight forwarders, carrier consolidation benchmarks, and Total Landed Cost calculation models.", durationMinutes: 30, isPublished: true, learningObjectives: ["Freight forwarder SLA vetting", "Air vs ocean landed cost modeling", "Customs duties & buffer math"] },
  { id: "class-42", moduleId: "mod-10", orderIndex: 5, lectureCode: "42", title: "Case Study", summary: "Applied industry case study analyzing end-to-end international trade documentation, LC discrepancies, and port clearance bottlenecks.", durationMinutes: 30, isPublished: true, learningObjectives: ["Import discrepancy diagnosis", "Landed cost sensitivity simulation", "Executive trade resolution"] },
  { id: "class-43", moduleId: "mod-10", orderIndex: 6, lectureCode: "43", title: "Financial Flows, Working Capital, Cost Control & Budgeting", summary: "Cash-to-Cash (C2C) cycle calculation, supply chain financing (reverse factoring), and logistics budgeting methods.", durationMinutes: 30, isPublished: true, learningObjectives: ["Cash-to-Cash cycle optimization", "Supply Chain Finance", "Zero-based logistics budgeting"] },
  { id: "class-44", moduleId: "mod-11", orderIndex: 1, lectureCode: "44", title: "Demand Planning, Forecasting, MRP & DRP Systems", summary: "Sales & Operations Planning (S&OP) monthly cycle, Material Requirements Planning (MRP I & II), and Distribution Requirements Planning (DRP).", durationMinutes: 30, isPublished: true, learningObjectives: ["Executive S&OP consensus process", "MRP explosion & netting logic", "DRP network calculation"] },
  { id: "class-45", moduleId: "mod-11", orderIndex: 2, lectureCode: "45", title: "ERP, SAP, IoT, AI, Big Data, Analytics & Blockchain", summary: "Enterprise Resource Planning (ERP/SAP) integration, IoT real-time cold-chain tracking, AI predictive demand, Big Data analytics, and Blockchain traceability.", durationMinutes: 30, isPublished: true, learningObjectives: ["SAP SCM module workflows", "IoT & AI predictive supply analytics", "Blockchain provenance tracking"] },
  { id: "class-46", moduleId: "mod-12", orderIndex: 1, lectureCode: "46", title: "Waste Reduction, Green SCM, CSR, Green Procurement & Eco-Design", summary: "Environmental compliance, waste reduction, circular supply chains, CSR, green procurement criteria, and Scope 1/2/3 carbon accounting.", durationMinutes: 30, isPublished: true, learningObjectives: ["Scope 1, 2, 3 carbon accounting", "Green procurement criteria", "Waste reduction & eco-design"] },
  { id: "class-47", moduleId: "mod-12", orderIndex: 2, lectureCode: "47", title: "Group Discussion & Course Review", summary: "Executive group discussions, capstone synthesis across all 12 modules, and comprehensive pre-final course review.", durationMinutes: 30, isPublished: true, learningObjectives: ["Collaborative capstone defense", "Cross-module strategic review", "Addressing stakeholder objections"] },
  { id: "class-48", moduleId: "mod-12", orderIndex: 3, lectureCode: "48", title: "Final Exam (100 Marks)", summary: "Official ISCEB PGD Final Comprehensive Examination synthesizing Foundation and Strategic Supply Chain Management (100 Marks).", durationMinutes: 60, isPublished: true, learningObjectives: ["End-to-end SCM mastery verification", "Quantitative & strategic synthesis", "PGD certification qualification"] }
];

export const INITIAL_EXAMS = [
  {
    id: "exam-mod-1", moduleId: "mod-1", title: "Module 1 Examination: Concept of SCM & Demand Planning (20 Marks)", totalMarks: 20, passingPercentage: 60, durationMinutes: 20,
    questions: [
      { id: "q1-1", question: "Which of the following describes the three foundational flows in a supply chain?", options: ["Material, Information, and Financial flows", "Production, Marketing, and Sales flows", "Import, Export, and Duty flows", "Warehousing, Shipping, and Billing flows"], correctOptionIndex: 0, marks: 5 },
      { id: "q1-2", question: "What is the primary consequence of uncoordinated demand signals along the supply tier?", options: ["Lower holding costs", "The Bullwhip Effect", "Immediate replenishment", "Zero lead time"], correctOptionIndex: 1, marks: 5 },
      { id: "q1-3", question: "In the SCOR framework, which process coordinates aggregate demand and supply?", options: ["Source", "Plan", "Make", "Deliver"], correctOptionIndex: 1, marks: 5 },
      { id: "q1-4", question: "Which document converts customer sales forecast into internal raw material purchasing needs?", options: ["Purchase Requisition (PR)", "Bill of Lading", "Commercial Invoice", "Work Order"], correctOptionIndex: 0, marks: 5 }
    ]
  },
  {
    id: "exam-mod-2", moduleId: "mod-2", title: "Module 2 Examination: Sourcing & Vendor Management (25 Marks)", totalMarks: 25, passingPercentage: 60, durationMinutes: 30,
    questions: [
      { id: "q2-1", question: "In strategic negotiation, what does the acronym BATNA represent?", options: ["Best Alternative to a Negotiated Agreement", "Basic Agreement Terms for National Agents", "Benchmark Analysis of Total Network Assets", "Bargaining Advantage Through Net Assets"], correctOptionIndex: 0, marks: 5 },
      { id: "q2-2", question: "Which evaluation metric assesses supplier performance based on price, quality, delivery, and service?", options: ["Vendor Balanced Scorecard", "Gantt Chart", "Break-Even Graph", "Pareto Analysis"], correctOptionIndex: 0, marks: 5 },
      { id: "q2-3", question: "What is the primary purpose of a formal Vendor Enlistment Process?", options: ["To eliminate all foreign suppliers", "To verify statutory compliance, financial viability, and operational capability", "To increase purchase order lead times", "To minimize inventory turnover"], correctOptionIndex: 1, marks: 5 },
      { id: "q2-4", question: "Total Cost of Ownership (TCO) in strategic sourcing includes:", options: ["Only the invoiced purchase price", "Purchase price plus acquisition, maintenance, operating, and disposal costs", "Only customs duty and freight charges", "Only warehousing storage rent"], correctOptionIndex: 1, marks: 5 },
      { id: "q2-5", question: "In supplier relationship management, which tactic is best suited for strategic partnership items?", options: ["Aggressive price spot bidding", "Long-term collaboration, risk sharing, and joint value creation", "Frequent vendor termination", "Informal verbal contracts"], correctOptionIndex: 1, marks: 5 }
    ]
  },
  {
    id: "exam-mod-3", moduleId: "mod-3", title: "Module 3 Examination: Procurement Local & International (30 Marks)", totalMarks: 30, passingPercentage: 60, durationMinutes: 35,
    questions: [
      { id: "q3-1", question: "Under Incoterms 2020 rules, under which term does the seller pay for carriage AND marine insurance to destination?", options: ["FOB (Free on Board)", "EXW (Ex Works)", "CIF (Cost, Insurance and Freight)", "CFR (Cost and Freight)"], correctOptionIndex: 2, marks: 6 },
      { id: "q3-2", question: "Which type of Letter of Credit cannot be cancelled or amended without the agreement of all parties?", options: ["Revocable LC", "Irrevocable LC", "Red Clause LC", "Transferable LC"], correctOptionIndex: 1, marks: 6 },
      { id: "q3-3", question: "What is the primary legal function of a clean Bill of Lading (B/L)?", options: ["Only a packing declaration", "Document of title, receipt of cargo by carrier, and evidence of contract of carriage", "A government export tax invoice", "A bank loan guarantee"], correctOptionIndex: 1, marks: 6 },
      { id: "q3-4", question: "What does an 8-digit Harmonized System (HS) code primarily determine in customs clearance?", options: ["Vessel sailing speed", "Applicable customs duties, regulatory duties, and VAT rates", "The buyer's bank credit score", "The number of container seals"], correctOptionIndex: 1, marks: 6 },
      { id: "q3-5", question: "What facility allows export-oriented manufacturing firms in Bangladesh to import raw materials duty-free?", options: ["Commercial Invoice Voucher", "Customs Bonded Warehouse / Bond License", "Sight Draft Endorsement", "Local Overdraft Facility"], correctOptionIndex: 1, marks: 6 }
    ]
  },
  {
    id: "exam-mod-6", moduleId: "mod-6", title: "Module 6 Examination: SOP & QMS (25 Marks)", totalMarks: 25, passingPercentage: 60, durationMinutes: 30,
    questions: [
      { id: "q6-1", question: "What is the primary role of a Standard Operating Procedure (SOP) in warehouse logistics?", options: ["To provide step-by-step instructions ensuring consistency, safety, and compliance", "To serve solely as marketing material", "To replace all training programs permanently", "To eliminate the need for supervisory staff"], correctOptionIndex: 0, marks: 5 },
      { id: "q6-2", question: "Which standard specifies requirements for a certified Quality Management System (QMS)?", options: ["ISO 14001", "ISO 9001", "ISO 45001", "ISO 22000"], correctOptionIndex: 1, marks: 5 },
      { id: "q6-3", question: "In lean continuous improvement, what does the PDCA cycle stand for?", options: ["Plan, Do, Check, Act", "Purchase, Deliver, Calculate, Audit", "Prepare, Distribute, Count, Authorize", "Plan, Delay, Cancel, Assess"], correctOptionIndex: 0, marks: 5 },
      { id: "q6-4", question: "When investigating an operational defect or shipping error, which root cause technique asks 'Why' iteratively?", options: ["The 5 Whys Analysis", "SWOT Analysis", "Gantt Breakdown", "Break-Even Chart"], correctOptionIndex: 0, marks: 5 },
      { id: "q6-5", question: "What is the required consequence when an incoming raw material shipment fails QMS inspection?", options: ["Immediate payment of invoice", "Issuance of Non-Conformance Report (NCR) and material quarantine", "Overlooking minor discrepancies", "Discarding all receiving records"], correctOptionIndex: 1, marks: 5 }
    ]
  },
  {
    id: "exam-final", moduleId: "mod-12", title: "Final Comprehensive Examination: PGD in SCM (100 Marks)", totalMarks: 100, passingPercentage: 60, durationMinutes: 60,
    questions: [
      { id: "qf-1", question: "Which mathematical formula defines the classical Economic Order Quantity (EOQ)?", options: ["Sqrt((2 * Annual Demand * Order Cost) / Holding Cost)", "(Demand * Lead Time) + Safety Stock", "Annual Demand / 365 * Unit Price", "Holding Cost / (2 * Order Cost)"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-2", question: "Under Incoterms 2020, at which precise point does risk transfer from seller to buyer in an FOB shipment?", options: ["When goods are placed on board the nominated vessel at loading port", "At seller's factory gate", "At destination discharge port", "When paying the bank LC"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-3", question: "Which international banking publication governs uniform documentary examination rules?", options: ["UCP 600", "Incoterms 2020", "WTO Trade Agreement", "ISO 9001"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-4", question: "Which document is legally mandatory for customs clearance in Bangladesh?", options: ["Bill of Entry (B/E)", "Certificate of Incorporation", "Bank Passbook", "Employee ID"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-5", question: "What is the primary objective of safety stock calculation?", options: ["To maintain a defined service level despite lead-time and demand variance", "To maximize depreciation", "To avoid supplier negotiations", "To fill empty aisles"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-6", question: "What is the key advantage of cross-docking?", options: ["Direct transfer from receiving to shipping minimizing holding costs", "90 days free bonded storage", "Doubling warehouse square footage", "Multiplying pallet touches"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-7", question: "What does a 3-way match in accounts payable verify prior to disbursement?", options: ["PO, receiving GRN, and vendor Invoice match in quantity & price", "Passport, bank balance, and signature", "Three supplier quotes", "B/L, AWB, and packing list"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-8", question: "In the Kraljic Matrix, what strategy suits 'Leverage Items'?", options: ["Competitive bidding and volume pooling across vendors", "Exclusive sole-sourcing contracts", "Accepting any price increase", "Holding 12 months safety stock"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-9", question: "How does ERP/SAP coordinate supply chain processes?", options: ["Unified single source of real-time data across functions", "Replacing all human employees", "Operating solely as email", "Eliminating transportation"], correctOptionIndex: 0, marks: 10 },
      { id: "qf-10", question: "What constitutes a circular closed-loop supply chain strategy?", options: ["Recovering end-of-life products for remanufacturing and recycling", "Disposing materials in landfills", "Exporting defective goods", "Purchasing virgin single-use inputs"], correctOptionIndex: 0, marks: 10 }
    ]
  }
];

export const INITIAL_ASSIGNMENTS = [
  { id: "asg-1", moduleId: "mod-1", title: "Module 1 Assignment: Demand Planning & Forecast Error", maxMarks: 20, isFinalProject: false, topicDescription: "Calculate 3-month moving average, MAD, and Purchase Requisition plan for an FMCG distributor." },
  { id: "asg-2", moduleId: "mod-2", title: "Module 2 Assignment: Strategic Vendor Scorecard & BATNA", maxMarks: 25, isFinalProject: false, topicDescription: "Formulate a weighted vendor evaluation scorecard and BATNA negotiation strategy." },
  { id: "asg-3", moduleId: "mod-3", title: "Module 3 Assignment: Import LC Discrepancy & Incoterms", maxMarks: 30, isFinalProject: false, topicDescription: "Review Proforma & Commercial Invoice under UCP 600 and recommend FOB vs CFR vs CIF for Chattogram Port." },
  { id: "asg-12-final-project", moduleId: "mod-12", title: "Final Project: End-to-End SCM Transformation Capstone Study", maxMarks: 50, isFinalProject: true, topicDescription: "Submit an executive capstone project covering supply chain diagnosis, EOQ/S&OP quantitative analysis, and implementation ROI." }
];

/**
 * Validates and constructs strict Firestore payloads that comply with firestore.rules
 * so PC/Desktop Admin updates sync 1:1 with Android ISCEB phones.
 */
export function buildFirestoreManifestPayload({
  authorUid,
  versionCode,
  versionTag,
  publishedBy,
  releaseNotes,
  announcement,
  latestApkVersionCode,
  latestApkVersionName,
  apkDownloadUrl,
  isCreate = false,
  serverTimestampToken = null
}) {
  const payload = {
    versionCode: Number(versionCode),
    versionTag: String(versionTag || "v1.5.0").slice(0, 64),
    publishedTimestamp: Date.now(),
    publishedBy: String(publishedBy || "Md. Nafiul Islam Nahin (Developer of ISCEB App)").slice(0, 200),
    releaseNotes: String(releaseNotes || "").slice(0, 4000),
    minSupportedAppVersion: 1,
    latestApkVersionCode: Number(latestApkVersionCode || 6),
    latestApkVersionName: String(latestApkVersionName || "1.5").slice(0, 64),
    apkDownloadUrl: String(apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl).slice(0, 1000)
  };
  if (announcement) {
    payload.announcement = String(announcement).slice(0, 2000);
  }
  if (isCreate) {
    payload.authorUid = String(authorUid);
    if (serverTimestampToken !== null) {
      payload.createdAt = serverTimestampToken;
    }
  }
  if (serverTimestampToken !== null) {
    payload.updatedAt = serverTimestampToken;
  }
  return payload;
}

export function buildFirestoreCatalogPayload({
  authorUid,
  versionCode,
  modules,
  classes,
  settings,
  founderProfile,
  developerProfile,
  members,
  isCreate = false,
  serverTimestampToken = null
}) {
  const safeSettings = {
    ...(settings || {}),
    institutionName: "ISCEB",
    officialWebsite: "www.isceb.org",
    founderProfileJson: JSON.stringify(founderProfile || DEFAULT_FOUNDER),
    developerProfileJson: JSON.stringify(developerProfile || DEFAULT_DEVELOPER),
    membersJson: JSON.stringify(members || DEFAULT_MEMBERS)
  };
  const payload = {
    versionCode: Number(versionCode),
    modules: (modules || INITIAL_MODULES).map((m) => ({
      id: m.id,
      number: Number(m.number),
      title: m.title,
      description: m.description,
      part: m.part,
      category: m.category || "Core SCM",
      estimatedHours: Number(m.estimatedHours || 4),
      iconName: m.iconName || "inventory",
      isRequired: m.isRequired !== false
    })),
    classes: (classes || INITIAL_CLASSES).map((c) => {
      const clsObj = {
        id: c.id,
        moduleId: c.moduleId,
        orderIndex: Number(c.orderIndex),
        lectureCode: c.lectureCode || "",
        title: c.title,
        summary: c.summary || "",
        durationMinutes: Number(c.durationMinutes || 30),
        isPublished: c.isPublished !== false,
        learningObjectives: Array.isArray(c.learningObjectives) ? c.learningObjectives : []
      };
      if (c.pdfMedia) clsObj.pdfMedia = c.pdfMedia;
      if (c.audioMedia) clsObj.audioMedia = c.audioMedia;
      if (c.videoMedia) clsObj.videoMedia = c.videoMedia;
      if (Array.isArray(c.attachments) && c.attachments.length > 0) {
        clsObj.attachments = c.attachments;
      }
      return clsObj;
    }),
    settings: safeSettings
  };
  if (isCreate) {
    payload.authorUid = String(authorUid);
    if (serverTimestampToken !== null) {
      payload.createdAt = serverTimestampToken;
    }
  }
  if (serverTimestampToken !== null) {
    payload.updatedAt = serverTimestampToken;
  }
  return payload;
}
