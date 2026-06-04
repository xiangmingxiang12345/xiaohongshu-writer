package com.xiaohongshu.writer.service;

import com.xiaohongshu.writer.dto.GenerateRequest;
import com.xiaohongshu.writer.dto.GenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GenerateService {

    private static final Logger log = LoggerFactory.getLogger(GenerateService.class);

    private final RestTemplate restTemplate;

    @Value("${ai.provider}")
    private String provider;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.claude.api-url}")
    private String claudeApiUrl;

    @Value("${ai.claude.model}")
    private String claudeModel;

    @Value("${ai.openai.api-url}")
    private String openaiApiUrl;

    @Value("${ai.openai.model}")
    private String openaiModel;

    public GenerateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GenerateResponse generate(GenerateRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API Key 未配置，返回模拟文案");
            return GenerateResponse.success(mockContent(request.getType(), request.getTopic()));
        }

        try {
            String content = "claude".equalsIgnoreCase(provider)
                    ? callClaude(request) : callOpenAI(request);
            return GenerateResponse.success(content);
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            return GenerateResponse.error(2, "AI 服务调用失败: " + e.getMessage());
        }
    }

    private String callClaude(GenerateRequest req) {
        String systemPrompt = buildSystemPrompt(req);        String userMessage = buildUserMessage(req);

        Map<String, Object> body = Map.of(
                "model", claudeModel,
                "max_tokens", 1024,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                claudeApiUrl + "/messages",
                new HttpEntity<>(body, headers),
                Map.class);

        Map data = resp.getBody();
        if (data == null || data.get("content") == null) {
            throw new RuntimeException("Claude 返回异常");
        }

        List<Map<String, Object>> contentList = (List<Map<String, Object>>) data.get("content");
        return (String) contentList.get(0).get("text");
    }

    private String callOpenAI(GenerateRequest req) {
        String systemPrompt = buildSystemPrompt(req);        String userMessage = buildUserMessage(req);

        Map<String, Object> body = Map.of(
                "model", openaiModel,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                openaiApiUrl + "/chat/completions",
                new HttpEntity<>(body, headers),
                Map.class);

        Map data = resp.getBody();
        if (data == null || data.get("choices") == null) {
            throw new RuntimeException("OpenAI 返回异常");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String buildSystemPrompt(GenerateRequest req) {
        String type = req.getType() != null ? req.getType() : "xiaohongshu";
        return switch (type) {
            case "douyin" ->
                "你是一位资深抖音短视频运营专家。根据用户提供的主题生成可直接拍摄的抖音视频文案。\n\n"
                + "要求：\n"
                + "1. 标题：吸引眼球的标题，15字以内\n"
                + "2. 口播文案：口语化、有节奏感、适合对着镜头说，200-400字\n"
                + "3. 画面建议：每个段落对应什么画面/镜头，用【】标注\n"
                + "4. 标签：3-5个热门话题标签\n"
                + "5. 整体风格：有钩子（前3秒抓住注意力），有共鸣，有互动引导\n\n"
                + "输出格式：\n"
                + "---\n"
                + "标题：xxx\n\n"
                + "口播文案：\n"
                + "xxx\n\n"
                + "画面建议：\n"
                + "【画面1】xxx\n"
                + "【画面2】xxx\n\n"
                + "标签：\n"
                + "#xxx #xxx #xxx\n"
                + "---";
            case "moments" ->
                "你是一位朋友圈文案专家。根据用户提供的主题生成适合发朋友圈的文案。\n\n"
                + "要求：\n"
                + "1. 简洁有温度，50-150字为宜\n"
                + "2. 语气自然，像朋友在分享生活\n"
                + "3. 适当使用 emoji，不要过度堆砌\n"
                + "4. 不需要标题和标签，直接是正文\n"
                + "5. 可搭配1-2条评论区的补充说明\n\n"
                + "输出格式：\n"
                + "---\n"
                + "正文：\n"
                + "xxx\n"
                + "---";
            case "weibo" ->
                "你是一位微博内容运营专家。根据用户提供的主题生成适合微博发布的文案。\n\n"
                + "要求：\n"
                + "1. 字数控制在140字以内，简洁有力\n"
                + "2. 有话题性，适合引发转发和讨论\n"
                + "3. 适当使用 emoji 和网络热词\n"
                + "4. 结尾可以带投票或提问引导互动\n"
                + "5. 带 2-3 个相关话题标签\n\n"
                + "输出格式：\n"
                + "---\n"
                + "正文：\n"
                + "xxx\n\n"
                + "标签：\n"
                + "#xxx #xxx #xxx\n"
                + "---";
            case "gongzhonghao" ->
                "你是一位资深公众号主笔。根据用户提供的主题生成一篇公众号文章。\n\n"
                + "要求：\n"
                + "1. 标题：吸引点击，20字以内，可用数字/悬念/痛点\n"
                + "2. 正文：结构清晰，800-1200字，有小标题分段\n"
                + "3. 风格：专业但不枯燥，有观点、有案例、有金句\n"
                + "4. 每个段落用 ## 小标题分隔\n"
                + "5. 结尾有总结或互动引导\n\n"
                + "输出格式：\n"
                + "---\n"
                + "标题：xxx\n\n"
                + "正文：\n"
                + "## xxx\n"
                + "xxx\n\n"
                + "## xxx\n"
                + "xxx\n"
                + "---";
            case "english" ->
                "You are an experienced English writing instructor and ESL specialist. Generate a well-written English essay based on the user's topic or prompt.\n\n"
                + "Requirements:\n"
                + "1. Title: engaging and relevant to the topic\n"
                + "2. Structure: clear introduction, body paragraphs, and conclusion\n"
                + "3. Language: natural, fluent English with varied sentence structures\n"
                + "4. Length: appropriate to the topic, typically 200-500 words\n"
                + "5. Content: coherent arguments, concrete examples, avoid repetition\n"
                + "6. For specific essay types (narrative/argumentative/descriptive/expository), follow the conventions of that type\n\n"
                + "Output format:\n"
                + "---\n"
                + "Title: xxx\n\n"
                + "Body:\n"
                + "xxx\n\n"
                + "---\n"
                + "💡 Writing Tips:\n"
                + "· xxx\n"
                + "· xxx\n"
                + "---";
            case "zuowen" ->
                "你是一位资深语文教师和作文辅导专家。根据用户提供的主题或题目生成一篇优秀范文。\n\n"
                + "要求：\n"
                + "1. 题目：紧扣主题，有文采，吸引人\n"
                + "2. 正文：结构完整，包含开头、主体、结尾，段落分明\n"
                + "3. 语言：用词优美，句式多样，适当使用修辞手法（比喻、排比、拟人等）\n"
                + "4. 字数：根据用户要求，一般 400-800 字\n"
                + "5. 内容：有真情实感，有具体细节，避免空话套话\n"
                + "6. 如有说明文体（记叙文/议论文/说明文），按对应文体规范写作\n\n"
                + "输出格式：\n"
                + "---\n"
                + "题目：xxx\n\n"
                + "正文：\n"
                + "xxx\n\n"
                + "---\n"
                + "💡 写作点拨：\n"
                + "· xxx\n"
                + "· xxx\n"
                + "---";
            case "taobao" ->
                "你是一位资深电商文案专家。根据用户提供的商品主题生成带货文案。\n\n"
                + "要求：\n"
                + "1. 标题：包含关键词+卖点，20字以内\n"
                + "2. 卖点提炼：3-5个核心卖点，每个卖点一句话\n"
                + "3. 详情描述：200-300字，解决用户痛点，引导下单\n"
                + "4. 适合场景：说明什么人群、什么场景下使用\n"
                + "5. 标签：3-5个搜索关键词\n\n"
                + "输出格式：\n"
                + "---\n"
                + "标题：xxx\n\n"
                + "卖点提炼：\n"
                + "❶ xxx\n"
                + "❷ xxx\n"
                + "❸ xxx\n\n"
                + "详情描述：\n"
                + "xxx\n\n"
                + "标签：\n"
                + "#xxx #xxx #xxx\n"
                + "---";
            default ->
                "你是一位资深小红书内容运营专家。根据用户提供的主题和（可选）图片，生成一篇可直接发布的小红书笔记。\n\n"
                + "要求：\n"
                + "1. 标题：吸引眼球，含数字或关键词，20字以内\n"
                + "2. 正文：口语化、有共鸣，多用短句和 emoji，300-500字\n"
                + "3. 标签：3-5个相关话题标签，如 #出租屋改造 #租房好物\n"
                + "4. 整体风格：真诚、有用、有互动感\n\n"
                + "输出格式：\n"
                + "---\n"
                + "标题：xxx\n\n"
                + "正文：\n"
                + "xxx\n\n"
                + "标签：\n"
                + "#xxx #xxx #xxx\n"
                + "---";
        };
    }

    private String buildUserMessage(GenerateRequest req) {
        String msg = "主题：" + req.getTopic();
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            msg += "\n\n用户还上传了 " + req.getImages().size() + " 张图片，请结合主题生成适合配图的文案。";
        }
        return msg;
    }

    private String mockContent(String type, String topic) {
        if (type == null) type = "xiaohongshu";
        return switch (type) {
            case "douyin" ->
                "标题：" + topic + "的秘密被发现了 😱\n\n"
                + "口播文案：\n"
                + "家人们！今天这条视频一定要看完！\n\n"
                + "关于" + topic + "，我敢说你 90% 的人都做错了 👇\n\n"
                + "第一个坑…（此处展开）\n"
                + "正确做法其实特别简单…\n\n"
                + "第二个坑…\n\n"
                + "觉得有用的话，点赞收藏别让这条视频划走了！\n"
                + "评论区告诉我你中了几个？\n\n"
                + "画面建议：\n"
                + "【开头】对着镜头直接说，表情要惊讶\n"
                + "【中间】展示操作画面/产品特写\n"
                + "【结尾】指屏幕，引导点赞评论\n\n"
                + "标签：\n"
                + "#" + topic + " #实用技巧 #干货分享 #知识";
            case "moments" ->
                "正文：\n"
                + "今天尝试了一下" + topic + "，没想到效果出乎意料的好 ☺️\n\n"
                + "有些东西真的要自己试过才知道，\n"
                + "之前一直犹豫要不要开始，\n"
                + "现在只能说—后悔没有早点行动！\n\n"
                + "愿我们都能勇敢迈出第一步 🫶\n\n"
                + "（图 1-3 是我拍的，原图无滤镜）";
            case "weibo" ->
                "正文：\n"
                + "关于" + topic + "，我真的有话说👇\n\n"
                + "试过很多方法，踩过很多坑，最后发现其实没那么复杂。\n"
                + "核心就三点：选对方向、坚持执行、及时复盘。\n\n"
                + "你们觉得呢？有什么踩坑经历评论区说说 🙋\n\n"
                + "标签：\n"
                + "#" + topic + " #经验分享 #今日话题 #吐槽";
            case "gongzhonghao" ->
                "标题：关于" + topic + "，我踩过的 5 个坑，每一个都是血泪教训\n\n"
                + "正文：\n"
                + "## 写在前面\n"
                + "大家好，我是 xxx。今天想跟大家聊聊" + topic + "这个话题。\n\n"
                + "在过去的两年里，我几乎把所有的坑都踩了一遍。\n"
                + "今天把这些经验分享出来，希望能帮大家少走弯路。\n\n"
                + "## 第一个坑：盲目跟风\n"
                + "很多人一开始看到什么火就跟什么，结果就是…\n\n"
                + "## 第二个坑：忽略基础\n"
                + "基础不牢，地动山摇。\n\n"
                + "## 写在最后\n"
                + "希望这篇文章对你有帮助。如果你也有类似经历，欢迎在评论区分享。\n\n"
                + "---\n"
                + "关注我，获取更多" + topic + "相关干货。";
            case "english" ->
                "Title: The Power of " + topic + "\n\n"
                + "Body:\n"
                + "    " + topic + " is a topic that has been widely discussed in recent years. "
                + "It plays a significant role in our daily lives and deserves our attention.\n\n"
                + "    First and foremost, " + topic + " helps us broaden our horizons. "
                + "By exploring this subject, we gain a deeper understanding of the world around us. "
                + "For example, many people have found that engaging with " + topic + " opens up new perspectives and opportunities that they had never imagined before.\n\n"
                + "    Furthermore, " + topic + " teaches us the value of perseverance. "
                + "As the saying goes, 'Rome was not built in a day.' "
                + "Achieving success in any field requires consistent effort and dedication, and " + topic + " is no exception.\n\n"
                + "    Last but not least, " + topic + " encourages creativity and critical thinking. "
                + "It challenges us to ask questions, seek answers, and think outside the box. "
                + "These are essential skills for personal growth and academic success.\n\n"
                + "    In conclusion, " + topic + " is more than just a subject — it is a journey of discovery. "
                + "By embracing it, we not only gain knowledge but also develop qualities that will benefit us for a lifetime.\n\n"
                + "---\n"
                + "💡 Writing Tips:\n"
                + "· Start with a strong thesis statement\n"
                + "· Use topic sentences to begin each paragraph\n"
                + "· Support your points with examples and evidence\n"
                + "· Use transition words (firstly, furthermore, in conclusion)\n"
                + "· End with a memorable concluding statement\n"
                + "---";
            case "zuowen" ->
                "题目：从" + topic + "说开去\n\n"
                + "正文：\n"
                + "　　" + topic + "，看似平常的三个字，细细品味，却蕴含着生活的真谛。\n\n"
                + "　　清晨的阳光透过窗帘洒进房间，我坐在窗前，脑海中浮现出与" + topic + "有关的点点滴滴。那些看似微不足道的瞬间，如同一颗颗珍珠，串联起我成长的轨迹。\n\n"
                + "　　记得那是一个秋天的午后…（此处展开具体经历）正是那一次经历，让我对" + topic + "有了全新的理解。原来，生活中有太多被我们忽略的美好，它们静静地等待着被发现、被珍惜。\n\n"
                + "　　正如古人所言：'不积跬步，无以至千里；不积小流，无以成江海。' " + topic + "教会我的，正是这种日积月累的力量。\n\n"
                + "　　在这个快节奏的时代，我们总是匆匆赶路，却忘了停下来看看路边的风景。或许，适时放慢脚步，用心感受身边的一草一木，才能发现生活最本真的模样。\n\n"
                + "　　" + topic + "，带给我的不仅是一段回忆，更是一种生活的态度。愿我们都能在平凡中发现不凡，在细微处感受美好。\n\n"
                + "---\n"
                + "💡 写作点拨：\n"
                + "· 开头从主题引入，设置情境\n"
                + "· 中间通过具体事例展开，有细节描写\n"
                + "· 引用名言增强说服力和文采\n"
                + "· 结尾升华主题，回扣开头\n"
                + "· 全文采用'总-分-总'结构，层次分明\n"
                + "---";
            case "taobao" ->
                "标题：" + topic + "- 性价比之选，用过就回不去！\n\n"
                + "卖点提炼：\n"
                + "❶ 高品质材料，耐用不易坏\n"
                + "❷ 设计人性化，使用体验极佳\n"
                + "❸ 价格亲民，一顿饭钱就能拥有\n\n"
                + "详情描述：\n"
                + "还在为" + topic + "纠结吗？\n"
                + "这款产品真的解决了我的大问题！\n\n"
                + "自从用了它，生活质量直接提升了一个档次 💯\n"
                + "操作简单，家里的老人小孩都能用。\n"
                + "现在下单还送赠品，限量 100 份！\n\n"
                + "标签：\n"
                + "#" + topic + " #好物推荐 #居家好物 #性价比";
            default ->
                "标题：" + topic + "的 3 个绝妙技巧，后悔没早知道 😭\n\n"
                + "正文：\n"
                + "姐妹们！今天必须把压箱底的经验分享出来 👇\n\n"
                + "关于" + topic + "，我真的踩了太多坑了🥲\n"
                + "试过各种方法，最后发现真正有用的就这几点：\n\n"
                + "1️⃣ 选对工具很重要\n"
                + "工欲善其事必先利其器，千万别在这上面省钱\n\n"
                + "2️⃣ 坚持才是王道\n"
                + "三天打鱼两天晒网真的不行，给自己定个小目标\n\n"
                + "3️⃣ 要学会借力\n"
                + "一个人摸索太慢了，多看看别人的经验分享\n\n"
                + "按照这 3 步走，真的会有意想不到的效果！\n"
                + "还有什么问题欢迎评论区问我～❤️\n\n"
                + "标签：\n"
                + "#" + topic + " #经验分享 #干货 #好物推荐 #生活技巧";
        };
    }
}
