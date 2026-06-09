import type { Locale } from "@/lib/dictionaries";

/**
 * Long-form legal copy (Privacy Policy + Terms of Service). Kept here rather
 * than in messages/*.json so the i18n dictionaries stay short and these
 * documents read as documents. Reflects the app's ACTUAL data practices:
 * Firebase Auth (email/uid), study data in our DB, Stripe for payments (we
 * never see card numbers), and the DeepSeek AI provider which receives only
 * question text — never your identity (no user_id / email), per the privacy
 * contract enforced in the backend AI layer.
 *
 * ⚠️ DRAFT — written to be close to compliant, NOT legal advice. Have a lawyer
 * review before relying on it in production, especially the minors / COPPA /
 * CCPA sections (the audience skews under 18).
 */

export type LegalSection = { heading: string; paragraphs: string[] };
export type LegalDoc = {
  title: string;
  updated: string;
  intro: string[];
  sections: LegalSection[];
  reviewNote: string;
};

const UPDATED_EN = "Last updated: June 8, 2026";
const UPDATED_ZH = "最后更新：2026 年 6 月 8 日";

const REVIEW_EN =
  "This is a working draft provided for transparency and is not legal advice. " +
  "It will be reviewed by counsel before launch.";
const REVIEW_ZH =
  "本文为出于透明目的提供的工作草稿，不构成法律意见，正式上线前将经法律审核。";

// ---------------------------------------------------------------------------
// Privacy Policy
// ---------------------------------------------------------------------------

const privacyEn: LegalDoc = {
  title: "Privacy Policy",
  updated: UPDATED_EN,
  intro: [
    "DMV Prep (“we”, “us”) helps you prepare for California DMV written exams. " +
      "This policy explains what we collect, why, and the choices you have. It applies to the " +
      "DMV Prep web app.",
  ],
  sections: [
    {
      heading: "Information we collect",
      paragraphs: [
        "Account information: your email address and a unique identifier from our authentication " +
          "provider (Google Firebase Authentication) when you sign in or register.",
        "Study data: your practice answers, mock-exam attempts, mistakes, and progress, so we can " +
          "track readiness and personalize what you practice next.",
        "Payment information: if you subscribe, payments are processed by Stripe. We do not collect " +
          "or store your full card number — Stripe handles it. We keep a record that a " +
          "subscription is active and which exam it covers.",
        "Technical data: basic device and log information, plus cookies / local storage used to keep " +
          "you signed in and remember preferences such as language.",
      ],
    },
    {
      heading: "AI explanations and your privacy",
      paragraphs: [
        "When you ask for an AI explanation, the question text and your selected answer are sent to " +
          "our AI provider (DeepSeek) to generate a response. We do NOT send your identity — no " +
          "user id, email, or account information leaves our servers with that request.",
      ],
    },
    {
      heading: "How we use your information",
      paragraphs: [
        "To provide and operate the service, personalize your practice, process subscriptions, " +
          "keep your account secure, and improve the product. We do not sell your personal information.",
      ],
    },
    {
      heading: "Who we share it with",
      paragraphs: [
        "Service providers who process data on our behalf: Google Firebase (authentication), Stripe " +
          "(payments), and DeepSeek (AI explanations — question text only, never your identity). " +
          "We may disclose information if required by law.",
      ],
    },
    {
      heading: "Data retention and your choices",
      paragraphs: [
        "You can reset your learning progress in the app, and you may request access to or deletion " +
          "of your account data by contacting us. We keep data while your account is active and as " +
          "needed to comply with our legal obligations.",
        "Depending on where you live, you may have rights to access, correct, delete, or port your " +
          "data, and to opt out of any sale of personal information (we do not sell it).",
      ],
    },
    {
      heading: "Children and minors",
      paragraphs: [
        "DMV Prep is intended for learners preparing for a driving exam. If you are under the age of " +
          "majority, you should use the service with the involvement of a parent or guardian. We do " +
          "not knowingly collect personal information from children under 13 without verifiable " +
          "parental consent; if you believe a child under 13 has provided us information, contact us " +
          "and we will delete it.",
      ],
    },
    {
      heading: "Security",
      paragraphs: [
        "We use industry-standard measures to protect your data, including encrypted connections and " +
          "scoped access. No method of transmission or storage is 100% secure.",
      ],
    },
    {
      heading: "Changes and contact",
      paragraphs: [
        "We may update this policy and will revise the date above when we do. Questions? Email " +
          "nanruihit@gmail.com.",
      ],
    },
  ],
  reviewNote: REVIEW_EN,
};

const privacyZh: LegalDoc = {
  title: "隐私政策",
  updated: UPDATED_ZH,
  intro: [
    "DMV 备考（“我们”）帮助你备考加州 DMV 笔试。本政策说明我们收集哪些信息、为何收集，以及你拥有的选择。" +
      "本政策适用于 DMV 备考网页应用。",
  ],
  sections: [
    {
      heading: "我们收集的信息",
      paragraphs: [
        "账户信息：当你登录或注册时，来自身份认证服务商（Google Firebase Authentication）的电子邮箱及唯一标识符。",
        "学习数据：你的练习作答、模拟考试记录、错题与进度，用于评估准备度并为你个性化推荐下一步练习。",
        "支付信息：若你订阅，付款由 Stripe 处理。我们不收集或存储你的完整卡号——由 Stripe 负责；我们仅保留" +
          "“订阅是否有效、覆盖哪个考试”的记录。",
        "技术数据：基本的设备与日志信息，以及用于保持登录状态、记住语言等偏好的 Cookie / 本地存储。",
      ],
    },
    {
      heading: "AI 解释与你的隐私",
      paragraphs: [
        "当你请求 AI 解释时，题目文本与你所选答案会发送给我们的 AI 服务商（DeepSeek）以生成回复。我们不会发送" +
          "你的身份信息——该请求不会将用户 id、邮箱或账户信息带出我们的服务器。",
      ],
    },
    {
      heading: "我们如何使用这些信息",
      paragraphs: [
        "用于提供与运营服务、个性化你的练习、处理订阅、保障账户安全以及改进产品。我们不出售你的个人信息。",
      ],
    },
    {
      heading: "我们与谁共享",
      paragraphs: [
        "代表我们处理数据的服务商：Google Firebase（身份认证）、Stripe（支付）、DeepSeek（AI 解释——仅题目文本，" +
          "绝不含你的身份）。在法律要求时我们可能披露信息。",
      ],
    },
    {
      heading: "数据保留与你的选择",
      paragraphs: [
        "你可在应用内重置学习进度，也可联系我们请求访问或删除你的账户数据。我们在账户有效期内以及为遵守法律义务所" +
          "需期间保留数据。",
        "依据你所在地区，你可能拥有访问、更正、删除或转移数据的权利，以及拒绝出售个人信息的权利（我们不出售）。",
      ],
    },
    {
      heading: "儿童与未成年人",
      paragraphs: [
        "DMV 备考面向准备驾照考试的学习者。若你未达法定成年年龄，应在父母或监护人参与下使用本服务。我们不会在未经" +
          "可验证的父母同意下，故意收集 13 岁以下儿童的个人信息；若你认为有 13 岁以下儿童向我们提供了信息，请联系" +
          "我们，我们将予以删除。",
      ],
    },
    {
      heading: "安全",
      paragraphs: [
        "我们采用业界标准措施保护你的数据，包括加密连接与最小化访问。但没有任何传输或存储方式是 100% 安全的。",
      ],
    },
    {
      heading: "变更与联系",
      paragraphs: [
        "我们可能更新本政策，并会同步更新上方日期。有疑问请发送邮件至 nanruihit@gmail.com。",
      ],
    },
  ],
  reviewNote: REVIEW_ZH,
};

// ---------------------------------------------------------------------------
// Terms of Service
// ---------------------------------------------------------------------------

const termsEn: LegalDoc = {
  title: "Terms of Service",
  updated: UPDATED_EN,
  intro: [
    "These terms govern your use of the DMV Prep web app. By using the service you agree to them.",
  ],
  sections: [
    {
      heading: "Who can use DMV Prep",
      paragraphs: [
        "You may use the service if you can form a binding agreement with us, or if you are a minor " +
          "using it with the consent and supervision of a parent or guardian.",
      ],
    },
    {
      heading: "Not affiliated with the DMV",
      paragraphs: [
        "DMV Prep is an independent study tool. It is not affiliated with, endorsed by, or operated " +
          "by the California DMV or any government agency. Practice questions are study aids and are " +
          "not official exam questions. We do not guarantee that you will pass any exam.",
      ],
    },
    {
      heading: "Your account",
      paragraphs: [
        "You are responsible for the activity under your account and for keeping your sign-in secure. " +
          "For sensitive actions (such as changing a subscription) we may ask you to re-confirm your " +
          "identity.",
      ],
    },
    {
      heading: "Subscriptions and billing",
      paragraphs: [
        "Some features require a paid subscription, billed per exam through Stripe. Subscriptions " +
          "renew until cancelled; you can cancel from your account, and cancellation takes effect at " +
          "the end of the current billing period. Prices and any refund terms are shown at checkout.",
      ],
    },
    {
      heading: "Acceptable use",
      paragraphs: [
        "Don’t misuse the service: no attempts to break security, scrape or copy the question " +
          "bank, resell access, or interfere with other users.",
      ],
    },
    {
      heading: "Content and intellectual property",
      paragraphs: [
        "The app, its content, and the question bank are owned by us or our licensors and are " +
          "provided for your personal study only.",
      ],
    },
    {
      heading: "Disclaimers and liability",
      paragraphs: [
        "The service is provided “as is” without warranties. To the extent permitted by law, " +
          "we are not liable for indirect or consequential damages, and our total liability is limited " +
          "to the amount you paid us in the prior twelve months.",
      ],
    },
    {
      heading: "Termination, governing law, and contact",
      paragraphs: [
        "We may suspend or end access for violations of these terms. These terms are governed by the " +
          "laws of the State of California. Questions? Email nanruihit@gmail.com.",
      ],
    },
  ],
  reviewNote: REVIEW_EN,
};

const termsZh: LegalDoc = {
  title: "服务条款",
  updated: UPDATED_ZH,
  intro: ["本条款规范你对 DMV 备考网页应用的使用。使用本服务即表示你同意这些条款。"],
  sections: [
    {
      heading: "谁可以使用 DMV 备考",
      paragraphs: [
        "若你能够与我们订立具有约束力的协议，或你是未成年人但在父母或监护人同意与监督下使用，你即可使用本服务。",
      ],
    },
    {
      heading: "与 DMV 无关联",
      paragraphs: [
        "DMV 备考是独立的学习工具，与加州 DMV 或任何政府机构无关联、未获其背书、也非其运营。练习题为学习辅助，并非" +
          "官方考题。我们不保证你能通过任何考试。",
      ],
    },
    {
      heading: "你的账户",
      paragraphs: [
        "你需对账户下的活动负责，并妥善保管登录凭证。对于敏感操作（如更改订阅），我们可能要求你重新确认身份。",
      ],
    },
    {
      heading: "订阅与计费",
      paragraphs: [
        "部分功能需付费订阅，按考试通过 Stripe 计费。订阅在取消前自动续订；你可在账户中取消，取消将于当前计费周期" +
          "结束时生效。价格及退款条款将在结账时显示。",
      ],
    },
    {
      heading: "可接受的使用",
      paragraphs: [
        "请勿滥用服务：不得尝试破坏安全、抓取或复制题库、转售访问权限，或干扰其他用户。",
      ],
    },
    {
      heading: "内容与知识产权",
      paragraphs: ["应用、其内容及题库归我们或我们的许可方所有，仅供你个人学习使用。"],
    },
    {
      heading: "免责声明与责任限制",
      paragraphs: [
        "本服务按“现状”提供，不附带任何保证。在法律允许范围内，我们不对间接或后果性损害负责，且我们的总责任以你在过去" +
          "十二个月内向我们支付的金额为限。",
      ],
    },
    {
      heading: "终止、适用法律与联系",
      paragraphs: [
        "对于违反本条款的行为，我们可暂停或终止访问。本条款受美国加利福尼亚州法律管辖。有疑问请发送邮件至 " +
          "nanruihit@gmail.com。",
      ],
    },
  ],
  reviewNote: REVIEW_ZH,
};

export const privacyContent: Record<Locale, LegalDoc> = { en: privacyEn, zh: privacyZh };
export const termsContent: Record<Locale, LegalDoc> = { en: termsEn, zh: termsZh };
