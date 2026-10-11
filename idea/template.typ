#import "@preview/cades:0.3.1"
#import "@preview/chronos:0.3.0"
#import "@preview/touying:0.8.0"

#let idea-title = "Stock & Queue Management for Retailers"

#import "theme.typ": idea-theme
#show: idea-theme.with(
  touying.config-info(
    title: idea-title,
    author: [Shubhrajyoti Biswas et. al., Code Clan],
    institution: [St. Xavier's College (Autonomous), Kolkata],
    date: datetime.today(),
  ),
)

// IMPORTANT INSTRUCTIONS
//
// * Please ensure below pointers are met while submitting the Idea PPT:
//   - Kindly keep the maximum slides (including the title slide) limit up to
//     six (6).
//   - Try to avoid paragraphs and post your idea in
//     points/diagrams/infographics/pictures.
//   - Keep your explanation precise and easy to understand.
//   - Idea should be unique and novel.
//   - You can only use provided template for making the PPT without changing
//     the idea details pointers (mentioned in previous slides).
//   - You need to save the file in PDF and upload the same on portal.

== Smart India Hackathon 2026

*Problem Statement ID:* SIH26179

*Problem Statement Title:* To build an AI-powered retail intelligence platform
that delivers real-time shopper analytics, automated inventory visibility, and
proactive queue management through on-device AI, enabling retailers to reduce
stock-outs, improve customer experience, optimize staffing, and increase
operational efficiency while maintaining privacy and minimizing cloud
dependency.

*Theme:* Miscellaneous

*PS Category:* Hardware

*Team ID:* 177588

*Team Name:* Code Clan

== #idea-title

// - Detailed explanation of the proposed solution
// - How it addresses the problem
// - Innovation and uniqueness of the solution 

#table(columns: (1fr, 1fr, 1fr), inset: 10pt, stroke: none)[
  *Existing Problems*
  - Targets _"Kirana"_ stores and community markets in small towns and
    villages.
  - Stores have *mechanical workflows*, very prone to errors.
  - Easy to *mismanage long queues* during rush, and discouraging customers.
][
  *Proposed Solution*
  - *Local network* installed in the shop itself.
  - Track *inventory* stock, and *orders* which are in process or processed.
  - Custom *POS hardware* to allow customers to interact with the system.
  - *Mobile app* to communicate with the POS.
][
  *Uniqueness*
  - The entire workflow can be *executed without an internet connection*.
  - *Computerization of stock and orders* enable easy data analysis.
]

== Technical Approach

// - Technologies to be used (e.g. programming languages, frameworks, hardware)
// - Methodology and process for implementation (flowcharts/images/prototypes)

#table(columns: (2fr, 3fr), inset: 10pt, stroke: none)[
  *Technologies*
  - Software
    - PostgreSQL _(Server DB)_
    - ReactJS _(Merchant UI)_
    - Jetpack Compose _(Cust. App)_
    - PlatformIO _(POS Embedded)_
  - Hardware
    - ESP32-CAM _(SoC & Camera)_
    - ILI9341 _(Display)_

  *Programming Languages*
  - C++, JavaScript, Kotlin, Python
  
  *Data Flow*
  - QR Codes, REST APIs, WebSockets
][
  #chronos.diagram({
    import chronos: *

    let w-par = (name, display-name, color) => _par(
      name,
      display-name: rect(fill: color, radius: 5pt, outset: 5pt)[#text(
        display-name,
        fill: white,
        weight: "bold",
      )],
      shape: "custom",
    )

    w-par("app", "Customer App", rgb("#40a02b"))
    w-par("pos", "POS", rgb("#209fb5"))
    w-par("server", "Merchant Server", rgb("#7287fd"))

    _seq("server", "pos", comment: smallcaps[inventory])
    _seq("pos", "app", comment: smallcaps[inventory state qr])
    _seq("app", "app", comment: smallcaps[cart])
    _seq("app", "pos", comment: smallcaps[order req. qr])
    _seq("pos", "server", comment: smallcaps[check avlbl.])
    _seq("pos", "app", comment: smallcaps[pay req.])
    _seq("app", "pos", comment: smallcaps[pay])
    _seq("pos", "server", comment: smallcaps[send order])
    _seq("server", "pos", comment: smallcaps[inventory (new)])
  })
]

== Feasilibity and Viability

// - Analysis of the feasibility of the idea
// - Potential challenges and risks
// - Strategies for overcoming these challenges

#table(columns: (1fr, 1fr), inset: 10pt, stroke: none)[
=== Feasilibity
  - This field is in *need of innovation*.
    _Not competing with e-commerce platforms!_
  - Data is structured in *versioned schema* issued by a regulatory body.
  - Supports *multiple POSes* by:
    - Separation of *Global Inventory* and *Terminal Inventory*.
    - *Load balancing of stock* after every successful order.
  - *Easy-to-use UI* for the merchant for studying analytics.
  - *Mobile app* ensures easy and quick adoption.
][
=== Challenges
  - *Concurrent purchases* may be able to overrun stock.
    - Attempt to *serialize purchases* by timestamp or inventory row locks.
    - Allow customer to *refresh stock from POS* if inventory is stale.
  - Proper *management of stock during rush* or scarcity situations.
    - *Anti-hoarding protocols* with heuristics or AI inference.
    - Prevents or *reduces hoarding and scalping*.
]

== Impact and Benefits

// - Potential impact on the target audience
// - Benefits of the solution (social, economic, environmental, etc.)

#table(columns: (1fr, 1fr, 1fr), inset: 10pt, stroke: none)[
  *Technological*
  - *Reusable by merchants*. Suited for small retailer stores, pharmacies,
    departmental stores, and small chains.
  - *Unified interface for customers*. Realized with adoption. Same app can be
    used in all stores.
  - *Works offline*. If cash is accepted by POSes, interaction works completely
    offline.
][
  *Social*
  - *Fairness in stock distribution*. Equal distribution of stock among POSes
    ensures just availability of stock among buyers.
  - *Encourages queueing up*. With single-user POS interactions, it forces
    customers to queue up, especially during rush.
][
  *Environmental*
  - *Improved stock monitoring*. For perishable goods, monitoring help realize
    whether items have perished or not. Additionally, the merchant server will
    prevent the sale of expired goods.
  - *Extendable*. POS terminals are hot-swappable. No need to reinvest in the
    ecosystem setup, only incremental upgrades.
]

== Research & References

#table(columns: (2fr, 1fr), inset: 10pt, stroke: none)[
  #let card-bg = rgb("c1eedd")
  #rect(radius: 10pt, inset: 30pt, width: 100%, height: 100%, fill: card-bg)[
    *Fealisibility Research*
    - BCG & NPCI: UPI and merchant digitization research.
    - BCG: India's connected-commerce ecosystem.
    - IBEF: Indian retail and MSME digitization reports.

    *Technological Research*
    - DENSO WAVE: QR Code Standard. #linebreak()
      (`https://qrcode.com`)
    - Kaggle: Blinkit Products Dataset, by Simranjeet Kaur. #linebreak()
      (`https://www.kaggle.com/datasets/simranjeetkaurtk/blinkit-products-dataset`)
  ]
][
  #let card-bg = rgb("#ddc1ee")
  #rect(radius: 10pt, inset: 30pt, width: 100%, height: 100%, fill: card-bg)[
    #align(center + horizon)[
      #text(size: 1.5em, weight: "bold")[`GitHub Repository`]
      #cades.qr-code(
        "https://github.com/chkrb/automatic-brocolli",
        background: card-bg,
      )
    ]
  ]
]
