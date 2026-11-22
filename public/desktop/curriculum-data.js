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
  educationBackground:
    "MBA in Finance; M.Com in Marketing; Post Graduate Diploma in Supply Chain Management (ISCEA, USA); global certifications under TUV SUD South Asia Pvt. Ltd., Business Express, and SELTA Training Associates (risk management, internal auditing, process validation, and international logistics).",
  biography:
    "Md. Amirul Islam is a seasoned Supply Chain Management professional with over two decades of proven expertise in leading and optimizing end-to-end supply chain operations across some of Bangladesh’s most respected conglomerates. He currently serves as the Head of Supply Chain Management at BRB Group, overseeing procurement, logistics, warehousing, planning, and supplier relationship management. Throughout his distinguished career, he held leadership positions at NIPRO JMI Company Ltd, Paradise Cables Ltd, Bengal Group of Industries, and Masud Steel Design BD Ltd. Beyond his corporate accomplishments, he serves as Lead Trainer at IECB and Founder & Lead Trainer of ISCEB with the vision: \"Connect. Learn. Achieve.\""
};

export const DEFAULT_DEVELOPER = {
  name: "Md. Nafiul Islam Nahin",
  designation: "Developer of ISCEB App",
  biography:
    "App Developer for ISCEB (Institute of Supply Chain Excellence Bangladesh), architecting Android, Web, and PC/Desktop platforms with real-time cloud synchronization, encrypted assessments, and verified executive diploma issuance.",
  contactEmail: "www.mdnafiulislam@gmail.com",
  location: "Dhaka, Bangladesh"
};

export const DEFAULT_MEMBERS_ROSTER_VERSION = 2;

export const DEFAULT_MEMBERS = [
  {
    id: "member-founder-amirul",
    builtInPhotoKey: "amirul",
    photoUrl: "./admin-amirul.webp",
    fullName: "Md. Amirul Islam",
    designation: "Founder of ISCEB",
    professionalIdentity: "Head of Supply Chain Management at BRB Group",
    currentOrganization: "BRB Group",
    educationalQualification:
      "MBA in Finance; M.Com in Marketing; Post Graduate Diploma in Supply Chain Management (ISCEA, USA). Additional professional certifications under TUV SUD South Asia Pvt. Ltd., Business Express, and SELTA Training Associates (risk management, internal auditing, process validation, and international logistics).",
    professionalExperience:
      "Over two decades of executive leadership across Bangladesh's top conglomerates. Currently Head of Supply Chain Management at BRB Group overseeing procurement, logistics, warehousing, planning, and supplier relationship management. Previously held key leadership roles at NIPRO JMI Company Ltd, Paradise Cables Ltd, Bengal Group of Industries, and Masud Steel Design BD Ltd. Also serves as Lead Trainer at IECB and Founder & Lead Trainer of ISCEB.",
    skillsAndExpertise: [
      "Strategic Sourcing & Vendor Management",
      "Bonded Warehouse Operations",
      "International Trade Compliance",
      "Demand Forecasting & Risk Management",
      "Internal Auditing & Process Validation",
      "Corporate Training & Mentorship"
    ],
    roleInIsceb: "Founder & Lead Trainer • Institute of Supply Chain Excellence Bangladesh (ISCEB)",
    contactInformation: "Mobile: 01715-282905 • Email: amirul.islam@isceb.org",
    biography:
      "Md. Amirul Islam is a seasoned Supply Chain Management professional with over two decades of proven expertise in leading and optimizing end-to-end supply chain operations across some of Bangladesh’s most respected conglomerates. He currently serves as the Head of Supply Chain Management at BRB Group, where he oversees procurement, logistics, warehousing, planning, and supplier relationship management to drive efficiency, innovation, and sustainable business growth.\n\nThroughout his distinguished career, Mr. Islam has held key leadership positions in reputed organizations such as NIPRO JMI Company Ltd, Paradise Cables Ltd, Bengal Group of Industries, and Masud Steel Design BD Ltd. His deep understanding of both local and international supply chain ecosystems has enabled him to deliver excellence in strategic sourcing, vendor management, bonded warehouse operations, international trade compliance, demand forecasting, and risk management.\n\nBeyond his corporate accomplishments, he serves as Lead Trainer at Industrial Engineering and Consultant Bangladesh (IECB) and Founder of ISCEB, inspiring the next generation of leaders with the vision: \"Connect. Learn. Achieve.\"",
    displayOrder: 1
  },
  {
    id: "member-hasina-khan",
    builtInPhotoKey: "hasina",
    photoUrl: "./member-hasina.webp",
    fullName: "Hasina Akter Khan",
    designation: "Chairman & CEO",
    professionalIdentity: "Chairman & CEO — Institute of Supply Chain Excellence Bangladesh (ISCEB)",
    currentOrganization: "Institute of Supply Chain Excellence Bangladesh (ISCEB)",
    educationalQualification: "M.Sc. in Clothing and Textile — University of Dhaka",
    professionalExperience:
      "Over 15 years of teaching and academic coordination experience. Formerly served as Teacher at Quality Learner’s High School and Viqarunnisa Noon School & College. Serves as Instructor and Course Co-Coordinator at Industrial Engineering & Consultants Bangladesh (IECB), and Chairman & CEO of ISCEB.",
    skillsAndExpertise: [
      "Academic Leadership",
      "Curriculum & Course Coordination",
      "Professional Capacity Building",
      "Industrial Education & Training",
      "Student Mentorship"
    ],
    roleInIsceb: "Chairman & Chief Executive Officer (CEO) • ISCEB",
    contactInformation: "Institute of Supply Chain Excellence Bangladesh (ISCEB) • www.isceb.org",
    biography:
      "Hasina Akter Khan is the Chairman and Chief Executive Officer (CEO) of the Institute of Supply Chain Excellence Bangladesh (ISCEB). She holds an M.Sc. in Clothing and Textile from the prestigious University of Dhaka and brings over 15 years of teaching and academic coordination experience to the field of education and professional development.\n\nBefore founding ISCEB, she served as a teacher at Quality Learner’s High School and Viqarunnisa Noon School & College, where she earned recognition for her dedication, academic excellence, and ability to inspire students. Her career reflects a deep commitment to empowering learners through knowledge, discipline, and creativity.\n\nIn addition to her leadership at ISCEB, Ms. Hasina Akter Khan serves as an Instructor and Course Co-Coordinator at Industrial Engineering & Consultants Bangladesh (IECB), contributing to training programs in industrial education, skills development, and professional capacity building. Her visionary leadership continues to guide ISCEB toward achieving its mission—to Connect, Learn, and Achieve through excellence in supply chain and professional education.",
    displayOrder: 2
  },
  {
    id: "member-masudur-rahman",
    builtInPhotoKey: "masudur",
    photoUrl: "./member-masudur.webp",
    fullName: "MD. MASUDUR RAHMAN",
    designation: "Manager - Import & Export, Logistics",
    professionalIdentity: "MD. MASUDUR RAHMAN Dip. SCM℗ (ITC), dCIPS • @ MR DIY Bangladesh",
    currentOrganization: "MR D.I.Y. (Bangladesh) Limited",
    educationalQualification: "Dip. SCM℗ (ITC), dCIPS — Supply Chain & International Procurement",
    professionalExperience:
      "11+ years of experience in Commercial, Import & Export Operations, International Procurement, Demand Planning, Vendor Management, and Supply Chain across Retail, Automobile, and Pharma industries. Currently Manager, Import & Export (Logistics, Commercial, Customs Clearance & Freight Forwarding) at MR D.I.Y. (Bangladesh) Limited (Malaysian MNC, Tejgaon, Dhaka, Oct 2023 – Present).",
    skillsAndExpertise: [
      "Import & Export Operations",
      "Foreign & Local Procurement",
      "LC Documentation & Customs Clearance",
      "Freight Forwarding & Landed Cost",
      "Demand Planning & Market Intelligence",
      "Vendor Management",
      "ERP & Digital Process Automation"
    ],
    roleInIsceb: "ISCEB Supply Chain & Commercial Operations Practitioner",
    contactInformation: "MR D.I.Y. (Bangladesh) Limited, Tejgaon, Dhaka • www.isceb.org",
    biography:
      "MD. MASUDUR RAHMAN (Dip. SCM℗ ITC, dCIPS) is an accomplished Supply Chain Professional with 11+ years of experience in Commercial, Import & Export Operations, International Procurement, Demand Planning, Vendor Management, and Supply Chain across the Retail, Automobile, and Pharma industries.\n\nSince October 2023, he has served as Manager, Import & Export at MR D.I.Y. (Bangladesh) Limited (a Malaysian MNC), managing end-to-end foreign and local raw material procurement, supplier negotiation, LC processing, shipment tracking, customs clearance, freight forwarding, landed cost evaluation, raw material market intelligence, ERP digital automation, and supply chain team mentorship.",
    displayOrder: 3
  },
  {
    id: "member-asif-mahmud",
    builtInPhotoKey: "asif",
    photoUrl: "./member-asif.webp",
    fullName: "Shah Mohammad Asif Mahmud",
    designation: "Faculty Tutor & SCM Leader",
    professionalIdentity: "CSCLTM, CSCMTM, CMILT, CEPSM, CEMIM, MBS, PGDSCM, PGDBM",
    currentOrganization: "Bitopi Group (Oculin Tech BD Ltd) • ISCEB Faculty",
    educationalQualification: "MBS, PGDSCM, PGDBM; Certified CSCLTM, CSCMTM, CMILT, CEPSM, CEMIM",
    professionalExperience:
      "Over 15 years of Supply Chain Management leadership:\n• Senior Manager, Supply Chain Management – Bitopi Group (Oculin Tech BD Ltd)\n• AGM, Supply Chain Management & Commercial – Omega Exim Ltd\n• In-Charge, Commercial & Procurement – ANC Medical Device BD Ltd\n• Assistant Manager, SCM – Summit Communications Ltd\n• Senior Executive (Commercial), Supply Chain – NIPRO JMI Company Ltd\n• Senior Executive, Procurement – Edison Group\n• Executive, Commercial – Spectrum Engineering Consortium Ltd",
    skillsAndExpertise: [
      "Procurement & Strategic Sourcing",
      "Commercial Operations (Import & Export)",
      "Logistics & Warehouse Management",
      "Distribution Network Strategy",
      "Vendor Relationship Management",
      "Case-Based SCM Instruction"
    ],
    roleInIsceb: "Faculty Tutor • Institute of Supply Chain Excellence Bangladesh (ISCEB)",
    contactInformation: "Faculty Tutor, Institute of Supply Chain Excellence Bangladesh (ISCEB) • www.isceb.org",
    biography:
      "With over 15 years of professional experience in Supply Chain Management, Shah Mohammad Asif Mahmud (CSCLTM, CSCMTM, CMILT, CEPSM, CEMIM, MBS, PGDSCM, PGDBM) is a highly accomplished and certified supply chain leader covering Procurement, Strategic Sourcing, Commercial Import & Export Operations, Logistics, Warehouse & Distribution Management, and Vendor Relationship Management.\n\nHaving led SCM teams at Bitopi Group, Omega Exim Ltd, ANC Medical Device BD Ltd, Summit Communications Ltd, NIPRO JMI Company Ltd, Edison Group, and Spectrum Engineering Consortium Ltd, he brings practical industry insights and hands-on case-based learning as a Tutor at ISCEB.",
    displayOrder: 4
  },
  {
    id: "member-mashiur-rahman",
    builtInPhotoKey: "mashiur",
    photoUrl: "./member-mashiur.webp",
    fullName: "Mr. Md. Mashiur Rahman",
    designation: "Sr. Consultant – HR",
    professionalIdentity: "A Visionary Leader Shaping the Future of Human Resources",
    currentOrganization: "Human Resources Consultancy & National Trainer",
    educationalQualification: "BBM & MBA (HR); Dhaka Cantonment School & College alumnus; 350+ local and international trainings completed",
    professionalExperience:
      "Over 22 years across prestigious international organizations in Hospital, Pharmaceuticals, Diagnostics, Medical Education, Medical Devices, Textiles, Real Estate, and Food & Beverage industries. Facilitated employment and career mentorship for 35,000+ individuals. Honored with a Global Award from the College of American Pathologists (CAP), USA for hospital audit and quality assurance leadership in HR.",
    skillsAndExpertise: [
      "Strategic Human Resource Management",
      "People-First Crisis Leadership",
      "Corporate Training & Career Coaching",
      "Hospital Audit & Quality Assurance",
      "Organizational Development",
      "Public Speaking & Mentorship"
    ],
    roleInIsceb: "Sr. Consultant – HR, Panelist & Corporate Trainer • ISCEB",
    contactInformation: "Institute of Supply Chain Excellence Bangladesh (ISCEB) • www.isceb.org",
    biography:
      "Mr. Md. Mashiur Rahman stands as a distinguished name in Human Resource Management — a visionary corporate professional, researcher, and dynamic leader redefining HR practices for over 22 years across healthcare, pharmaceuticals, diagnostics, medical education, textiles, real estate, and food & beverage.\n\nHe has personally facilitated career opportunities for over 35,000 individuals and earned global recognition, including an award from the College of American Pathologists (CAP), USA for contributions to hospital audit and quality assurance. An accomplished trainer, panelist, career coach, and author of HR columns across national media, he is also an engaging vocalist, stand-up comedian, scriptwriter, and cook.",
    displayOrder: 5
  },
  {
    id: "member-shajibul-hasan",
    builtInPhotoKey: "shajibul",
    photoUrl: "./member-shajibul.webp",
    fullName: "Shajibul Hasan",
    designation: "Database, BI & Data Analysis Expert",
    professionalIdentity: "Database Management, Business Intelligence & Data Analysis Expert",
    currentOrganization: "MIS, Business Intelligence & Analytics Consulting",
    educationalQualification:
      "Master’s in Applied Statistics and Data Science (Jahangirnagar University); B.Sc. in Computer Science and Engineering (ULAB); PGD on MEAL (Academy of Business Professionals - ABP); PGD on SCM (BIHRM)",
    professionalExperience:
      "Over 15 years of experience in Management Information Systems (MIS), software and database consulting, business intelligence, ERP implementation, survey design, and data analytics across UN specialized agencies, international NGOs, software organizations, corporates, and the manufacturing sector.",
    skillsAndExpertise: [
      "Database Management",
      "Business Intelligence (BI)",
      "Applied Statistics & Data Analytics",
      "MIS Reporting & ERP Implementation",
      "Survey Design & MEAL Systems",
      "Data-Driven Supply Chain Strategy"
    ],
    roleInIsceb: "Database Management, BI & Data Analytics Specialist • ISCEB",
    contactInformation: "Institute of Supply Chain Excellence Bangladesh (ISCEB) • www.isceb.org",
    biography:
      "Shajibul Hasan is a dedicated, results-driven Database Management, Business Intelligence, and Data Analysis Expert with over 15 years of experience spanning UN specialized agencies, international NGOs, the software industry, corporate organizations, and manufacturing.\n\nAcademically grounded with a Master’s in Applied Statistics and Data Science (Jahangirnagar University), a B.Sc. in CSE (ULAB), PGD on MEAL (ABP), and PGD on SCM (BIHRM), he specializes in building integrated MIS reporting architectures, ERP solutions, and scalable analytics systems.",
    displayOrder: 6
  },
  {
    id: "member-tonmoy-khan",
    builtInPhotoKey: "tonmoy",
    photoUrl: "./member-tonmoy.webp",
    fullName: "Tonmoy Khan",
    designation: "ISCEB Web Developer",
    professionalIdentity: "Senior Web Developer & Certified Plugin Developer (DIU CSE Alumnus)",
    currentOrganization: "Institute of Supply Chain Excellence Bangladesh (ISCEB)",
    educationalQualification: "B.Sc. in Computer Science & Engineering (CSE) — Daffodil International University (DIU)",
    professionalExperience:
      "4+ years of hands-on experience in core IT environments as a Senior Web Developer and certified plugin developer building scalable, high-performance web applications.",
    skillsAndExpertise: [
      "Full-Stack Web Engineering",
      "Certified Plugin Development",
      "Scalable Web Architecture",
      "Performance Optimization",
      "Seamless UI/UX Engineering"
    ],
    roleInIsceb: "ISCEB Web Developer",
    contactInformation: "ISCEB Web Engineering • www.isceb.org",
    biography:
      "Tonmoy Khan serves as the ISCEB Web Developer. As a Senior Web Developer and DIU CSE alumnus with 4+ years of hands-on experience in core IT environments and certified plugin development, he specializes in building scalable, high-performance solutions that bridge the gap between complex code and seamless user experiences across the web ecosystem.",
    displayOrder: 7
  },
  {
    id: "member-developer-nafiul",
    builtInPhotoKey: "nafiul",
    photoUrl: "./admin-nafiul.webp",
    fullName: "Developer Mohammad Nafiul Islam",
    designation: "Developer of ISCEB App",
    professionalIdentity: "Md. Nafiul Islam Nahin • Lead Android, Web & Cloud LMS Architect",
    currentOrganization: "ISCEB App Engineering & Cloud Systems",
    educationalQualification: "Android Systems Engineering (Kotlin / Jetpack Compose) & Cloud-Connected LMS Architecture",
    professionalExperience:
      "Lead architect and developer of the ISCEB LMS platform across Android and Web/PC Portals, designing real-time Cloud Firestore synchronization, role-based student/teacher/admin workflows, study telemetry, timed examinations, and QR-verifiable automated PDF executive diploma certification.",
    skillsAndExpertise: [
      "Android Kotlin & Jetpack Compose",
      "Cloud Firestore & Security Rules",
      "PC/Web PWA Portal Engineering",
      "Verifiable Digital Certification",
      "EdTech & Supply Chain LMS Architecture"
    ],
    roleInIsceb: "App Developer & Chief Systems Architect • ISCEB",
    contactInformation: "Email: www.mdnafiulislam@gmail.com • Web: https://www.isceb.org • Dhaka, Bangladesh",
    biography:
      "Developer Mohammad Nafiul Islam (Md. Nafiul Islam Nahin) is the Lead App Developer of the Institute of Supply Chain Excellence Bangladesh (ISCEB) LMS. He architected and engineered the unified Android and PC/Web platforms with offline-first persistence, live over-the-air curriculum publishing, role-scoped faculty and student dashboards, and automated QR-verified PGD certification.",
    displayOrder: 8
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

export const MAX_CLOUD_FILE_BYTES = 18 * 1024 * 1024;
export const LESSON_FILE_CHUNK_RAW_BYTES = 400_000;

export function uint8ArrayToBase64(bytes) {
  let binary = "";
  const sliceSize = 0x8000;
  for (let i = 0; i < bytes.length; i += sliceSize) {
    const sub = bytes.subarray(i, Math.min(i + sliceSize, bytes.length));
    binary += String.fromCharCode.apply(null, sub);
  }
  return btoa(binary);
}

export function splitBytesIntoLessonFileChunks({
  fileId,
  bytes,
  authorUid,
  serverTimestampToken = null
}) {
  if (!(bytes instanceof Uint8Array) || bytes.length === 0) {
    throw new Error("File is empty.");
  }
  if (bytes.length > MAX_CLOUD_FILE_BYTES) {
    throw new Error("File exceeds the 18 MB cloud sync limit.");
  }
  const safeFileId = String(fileId || "").replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 100);
  if (!safeFileId) throw new Error("Invalid fileId");

  const totalChunks = Math.ceil(bytes.length / LESSON_FILE_CHUNK_RAW_BYTES);
  if (totalChunks < 1 || totalChunks > 50) {
    throw new Error(`Chunk count ${totalChunks} exceeds Firestore limit (max 50).`);
  }

  const chunks = [];
  for (let index = 0; index < totalChunks; index++) {
    const start = index * LESSON_FILE_CHUNK_RAW_BYTES;
    const end = Math.min(start + LESSON_FILE_CHUNK_RAW_BYTES, bytes.length);
    const dataBase64 = uint8ArrayToBase64(bytes.subarray(start, end));
    const payload = {
      fileId: safeFileId,
      chunkIndex: index,
      totalChunks,
      dataBase64,
      authorUid: String(authorUid)
    };
    if (serverTimestampToken !== null) {
      payload.createdAt = serverTimestampToken;
      payload.updatedAt = serverTimestampToken;
    }
    chunks.push({
      chunkId: `${safeFileId}_${index}`,
      payload
    });
  }
  return chunks;
}

function sanitizeMediaForFirestore(media) {
  if (!media || typeof media !== "object") return media;
  const rawUrl = String(media.url || "").trim();
  return {
    ...media,
    url: rawUrl.startsWith("blob:") ? "" : rawUrl
  };
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
      if (c.pdfMedia) clsObj.pdfMedia = sanitizeMediaForFirestore(c.pdfMedia);
      if (c.audioMedia) clsObj.audioMedia = sanitizeMediaForFirestore(c.audioMedia);
      if (c.videoMedia) clsObj.videoMedia = sanitizeMediaForFirestore(c.videoMedia);
      if (Array.isArray(c.attachments) && c.attachments.length > 0) {
        clsObj.attachments = c.attachments.map(sanitizeMediaForFirestore);
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
