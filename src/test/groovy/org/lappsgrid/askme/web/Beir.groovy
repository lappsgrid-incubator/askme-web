package org.lappsgrid.askme.web

import groovy.json.*
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.api.Status
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.askme.core.model.Token
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Serializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

/**
 *
 */
class Beir {

    // Set this to nfcorpus or trec-covid
//    static final String DOMAIN = "trec-covid"
    static final String DOMAIN = "nfcorpus"

    public static final String TREC_COVID_QUESTIONS = """
{"1": "what is the origin of COVID-19", "2": "how does the coronavirus respond to changes in the weather", "3": "will SARS-CoV2 infected people develop immunity? Is cross protection possible?", "4": "what causes death from Covid-19?", "5": "what drugs have been active against SARS-CoV or SARS-CoV-2 in animal studies?", "6": "what types of rapid testing for Covid-19 have been developed?", "7": "are there serological tests that detect antibodies to coronavirus?", "8": "how has lack of testing availability led to underreporting of true incidence of Covid-19?", "9": "how has COVID-19 affected Canada", "10": "has social distancing had an impact on slowing the spread of COVID-19?", "11": "what are the guidelines for triaging patients infected with coronavirus?", "12": "what are best practices in hospitals and at home in maintaining quarantine?", "13": "what are the transmission routes of coronavirus?", "14": "what evidence is there related to COVID-19 super spreaders", "15": "how long can the coronavirus live outside the body", "16": "how long does coronavirus remain stable  on surfaces?", "17": "are there any clinical trials available for the coronavirus", "18": "what are the best masks for preventing infection by Covid-19?", "19": "what type of hand sanitizer is needed to destroy Covid-19?", "20": "are patients taking Angiotensin-converting enzyme inhibitors (ACE) at increased risk for COVID-19?", "21": "what are the mortality rates overall and in specific populations", "22": "are cardiac complications likely in patients with COVID-19?", "23": "what kinds of complications related to COVID-19 are associated with hypertension?", "24": "what kinds of complications related to COVID-19 are associated with diabetes", "25": "which biomarkers predict the severe clinical course of 2019-nCOV infection?", "26": "what are the initial symptoms of Covid-19?", "27": "what is known about those infected with Covid-19 but are asymptomatic?", "28": "what evidence is there for the value of hydroxychloroquine in treating Covid-19?", "29": "which SARS-CoV-2 proteins-human proteins interactions indicate potential for drug targets. Are there approved drugs that can be repurposed based on this information?", "30": "is remdesivir an effective treatment for COVID-19", "31": "How does the coronavirus differ from seasonal flu?", "32": "Does SARS-CoV-2 have any subtypes, and if so what are they?", "33": "What vaccine candidates are being tested for Covid-19?", "34": "What are the longer-term complications of those who recover from COVID-19?", "35": "What new public datasets are available related to COVID-19?", "36": "What is the protein structure of the SARS-CoV-2 spike?", "37": "What is the result of phylogenetic analysis of SARS-CoV-2 genome sequence?", "38": "What is the mechanism of inflammatory response and pathogenesis of COVID-19 cases?", "39": "What is the mechanism of cytokine storm syndrome on the COVID-19?", "40": "What are the observed mutations in the SARS-CoV-2 genome and how often do the mutations occur?", "41": "What are the impacts of COVID-19 among African-Americans that differ from the rest of the U.S. population?", "42": "Does Vitamin D impact COVID-19 prevention and treatment?", "43": "How has the COVID-19 pandemic impacted violence in society, including violent crimes?", "44": "How much impact do masks have on preventing the spread of the COVID-19?", "45": "How has the COVID-19 pandemic impacted mental health?", "46": "what evidence is there for dexamethasone as a treatment for COVID-19?", "47": "what are the health outcomes for children who contract COVID-19?", "48": "what are the benefits and risks of re-opening schools in the midst of the COVID-19 pandemic?", "49": "do individuals who recover from COVID-19 show sufficient immune response, including antibody levels and T-cell mediated immunity, to prevent re-infection?", "50": "what is known about an mRNA vaccine for the SARS-CoV-2 virus?"}
"""

    public static final String NFCORPUS_QUESTIONS = """
{"PLAIN-2": "Do Cholesterol Statin Drugs Cause Breast Cancer?", "PLAIN-12": "Exploiting Autophagy to Live Longer", "PLAIN-23": "How to Reduce Exposure to Alkylphenols Through Your Diet", "PLAIN-33": "What\\u2019s Driving America\\u2019s Obesity Problem?", "PLAIN-44": "Who Should be Careful About Curcumin?", "PLAIN-56": "Foods for Glaucoma", "PLAIN-68": "What is Actually in Chicken Nuggets?", "PLAIN-78": "What Do Meat Purge and Cola Have in Common?", "PLAIN-91": "Chronic Headaches and Pork Parasites", "PLAIN-102": "Stopping Heart Disease in Childhood", "PLAIN-112": "Food Dyes and ADHD", "PLAIN-123": "How Citrus Might Help Keep Your Hands Warm", "PLAIN-133": "Starving Tumors of Their Blood Supply", "PLAIN-143": "Are Dental X-Rays Safe?", "PLAIN-153": "How Should I Take Probiotics?", "PLAIN-165": "Breast Cancer & Alcohol: How Much is Safe?", "PLAIN-175": "Diet and Cellulite", "PLAIN-186": "Best Treatment for Constipation", "PLAIN-196": "Should We Avoid Titanium Dioxide?", "PLAIN-207": "Avoiding Cooked Meat Carcinogens", "PLAIN-217": "Plant-Based Diets for Psoriasis", "PLAIN-227": "Increasing Muscle Strength with Fenugreek", "PLAIN-238": "How Chemically Contaminated Are We?", "PLAIN-248": "Treating an Enlarged Prostate With Diet", "PLAIN-259": "Optimal Phytosterol Dose and Source", "PLAIN-270": "Is Caffeinated Tea Really Dehydrating?", "PLAIN-280": "Mercury Testing Recommended Before Pregnancy", "PLAIN-291": "Stool Size and Breast Cancer Risk", "PLAIN-307": "Vitamin D: Shedding some light on the new recommendations", "PLAIN-320": "Breast Cancer and Diet", "PLAIN-332": "Can antioxidant-rich spices counteract the effects of a high-fat meal?", "PLAIN-344": "Dioxins Stored in Our Own Fat May Increase Diabetes Risk", "PLAIN-358": "Didn't another study show carnitine was good for the heart?", "PLAIN-371": "Any update on the scary in vitro avocado data?", "PLAIN-383": "What do you think of Dr. Jenkins' take on paleolithic diets?", "PLAIN-395": "What about pepper plus turmeric in V8 juice?", "PLAIN-407": "Is annatto food coloring safe?", "PLAIN-418": "Fresh fruit versus frozen--which is better?", "PLAIN-430": "Are krill oil supplements better than fish oil capsules?", "PLAIN-441": "Is apple cider vinegar good for you?", "PLAIN-457": "How can you believe in any scientific study?", "PLAIN-468": "Is vitamin D3 (cholecalciferol) preferable to D2 (ergocalciferol)?", "PLAIN-478": "accidents", "PLAIN-488": "adenovirus 36", "PLAIN-499": "African-American", "PLAIN-510": "airport scanners", "PLAIN-520": "Alli", "PLAIN-531": "alternative medicine", "PLAIN-541": "American Dental Association", "PLAIN-551": "amnesia", "PLAIN-561": "aneurysm", "PLAIN-571": "anisakis", "PLAIN-583": "antinutrients", "PLAIN-593": "apnea", "PLAIN-603": "Arkansas", "PLAIN-613": "ascorbic acid", "PLAIN-623": "Atkins diet", "PLAIN-634": "avocados", "PLAIN-645": "bagels", "PLAIN-660": "beans", "PLAIN-671": "benzene", "PLAIN-681": "betel nuts", "PLAIN-691": "bioavailability", "PLAIN-701": "black raspberries", "PLAIN-711": "blood clots", "PLAIN-721": "BMAA", "PLAIN-731": "bone fractures", "PLAIN-741": "BPH", "PLAIN-751": "BRCA genes", "PLAIN-761": "breast pain", "PLAIN-771": "bronchiolitis obliterans", "PLAIN-782": "Bush administration", "PLAIN-792": "cadaverine", "PLAIN-806": "caloric restriction", "PLAIN-817": "canker sores", "PLAIN-827": "carcinogens", "PLAIN-838": "carrageenan", "PLAIN-850": "cauliflower", "PLAIN-872": "chanterelle mushrooms", "PLAIN-882": "Chernobyl", "PLAIN-892": "chickpeas", "PLAIN-902": "chlorophyll", "PLAIN-913": "cinnamon", "PLAIN-924": "cocaine", "PLAIN-934": "coffee", "PLAIN-946": "coma", "PLAIN-956": "cooking methods", "PLAIN-966": "cortisol", "PLAIN-977": "crib death", "PLAIN-987": "cumin", "PLAIN-997": "Czechoslovakia", "PLAIN-1008": "deafness", "PLAIN-1018": "DHA", "PLAIN-1028": "dietary scoring", "PLAIN-1039": "domoic acid", "PLAIN-1050": "Dr. Dean Ornish", "PLAIN-1066": "Dr. Walter Willett", "PLAIN-1088": "ECMO", "PLAIN-1098": "eggnog", "PLAIN-1109": "endocrine disruptors", "PLAIN-1119": "energy drinks", "PLAIN-1130": "ergothioneine", "PLAIN-1141": "Evidence-based medicine", "PLAIN-1151": "factory farming practices", "PLAIN-1161": "fava beans", "PLAIN-1172": "fenugreek", "PLAIN-1183": "Finland", "PLAIN-1193": "flax oil", "PLAIN-1203": "folic acid", "PLAIN-1214": "Fosamax", "PLAIN-1225": "fructose", "PLAIN-1236": "galactosemia", "PLAIN-1249": "genetic manipulation", "PLAIN-1262": "Global Burden of Disease Study", "PLAIN-1275": "goji berries", "PLAIN-1288": "grapes", "PLAIN-1299": "growth promoters", "PLAIN-1309": "halibut", "PLAIN-1320": "Harvard Physicians\\u2019 Study II", "PLAIN-1331": "hearing", "PLAIN-1342": "heme iron", "PLAIN-1353": "hernia", "PLAIN-1363": "Hiroshima", "PLAIN-1374": "hormonal dysfunction", "PLAIN-1387": "hyperactivity", "PLAIN-1398": "IGF-1", "PLAIN-1409": "industrial toxins", "PLAIN-1419": "insects", "PLAIN-1429": "Iowa Women\\u2019s Health Study", "PLAIN-1441": "Japan", "PLAIN-1453": "junk food", "PLAIN-1463": "kidney beans", "PLAIN-1473": "kohlrabi", "PLAIN-1485": "lard", "PLAIN-1496": "leeks", "PLAIN-1506": "leucine", "PLAIN-1516": "Lindane", "PLAIN-1527": "liver disease", "PLAIN-1537": "low-carb diets", "PLAIN-1547": "lyme disease", "PLAIN-1557": "magnesium", "PLAIN-1568": "maple syrup", "PLAIN-1579": "mastitis", "PLAIN-1590": "medical ethics", "PLAIN-1601": "memory", "PLAIN-1611": "mesquite", "PLAIN-1621": "Mevacor", "PLAIN-1635": "milk", "PLAIN-1645": "molasses", "PLAIN-1656": "mouth cancer", "PLAIN-1667": "muscle health", "PLAIN-1679": "myelopathy", "PLAIN-1690": "National Academy of Sciences", "PLAIN-1700": "Native Americans", "PLAIN-1710": "neurocysticercosis", "PLAIN-1721": "NIH-AARP study", "PLAIN-1731": "norovirus", "PLAIN-1741": "nuts", "PLAIN-1752": "okra", "PLAIN-1762": "oral intraepithelial neoplasia", "PLAIN-1772": "organotins", "PLAIN-1784": "oxen meat", "PLAIN-1794": "Panama", "PLAIN-1805": "Parkinson's disease", "PLAIN-1817": "peanut butter", "PLAIN-1827": "Peoria", "PLAIN-1837": "pesticides", "PLAIN-1847": "philippines", "PLAIN-1857": "phytic acid", "PLAIN-1867": "pineapples", "PLAIN-1877": "plant-based diet", "PLAIN-1887": "poisonous plants", "PLAIN-1897": "polypropylene plastic", "PLAIN-1909": "pork", "PLAIN-1919": "poultry workers", "PLAIN-1929": "prenatal vitamins", "PLAIN-1940": "prolactin", "PLAIN-1950": "prunes", "PLAIN-1962": "pumpkin", "PLAIN-1972": "quinine", "PLAIN-1983": "rapamycin", "PLAIN-1995": "red tea", "PLAIN-2009": "rhabdomyolysis", "PLAIN-2019": "rickets", "PLAIN-2030": "Rutin", "PLAIN-2040": "salmon", "PLAIN-2051": "saturated fat", "PLAIN-2061": "seafood", "PLAIN-2071": "serotonin", "PLAIN-2081": "shelf life", "PLAIN-2092": "sirtuins", "PLAIN-2102": "smoking", "PLAIN-2113": "soil health", "PLAIN-2124": "spearmint", "PLAIN-2134": "Splenda", "PLAIN-2145": "St. John's wort", "PLAIN-2156": "stevia", "PLAIN-2167": "subsidies", "PLAIN-2177": "sulfur", "PLAIN-2187": "suppositories", "PLAIN-2197": "sweeteners", "PLAIN-2209": "taro", "PLAIN-2220": "tempeh", "PLAIN-2230": "thiamine", "PLAIN-2240": "titanium dioxide", "PLAIN-2250": "tongue worm", "PLAIN-2261": "trans fats", "PLAIN-2271": "Tufts", "PLAIN-2281": "turnips", "PLAIN-2291": "ultra-processed foods", "PLAIN-2301": "uterine health", "PLAIN-2311": "veal", "PLAIN-2321": "veggie chicken", "PLAIN-2332": "viral infections", "PLAIN-2343": "vitamin K", "PLAIN-2354": "walnut oil", "PLAIN-2364": "weight gain", "PLAIN-2375": "whiting", "PLAIN-2386": "worms", "PLAIN-2396": "Yale", "PLAIN-2408": "Zoloft", "PLAIN-2430": "Preventing Brain Loss with B Vitamins?", "PLAIN-2440": "More Than an Apple a Day: Combating Common Diseases", "PLAIN-2450": "Are Organic Foods Safer?", "PLAIN-2460": "Diabetes as a Disease of Fat Toxicity", "PLAIN-2470": "Is Milk Good for Our Bones?", "PLAIN-2480": "Preventing Ulcerative Colitis with Diet", "PLAIN-2490": "The Actual Benefit of Diet vs. Drugs", "PLAIN-2500": "The Saturated Fat Studies: Buttering Up the Public", "PLAIN-2510": "Coffee and Artery Function", "PLAIN-2520": "Caloric Restriction vs. Plant-Based Diets", "PLAIN-2530": "Infectobesity: Adenovirus 36 and Childhood Obesity", "PLAIN-2540": "Does Cholesterol Size Matter?", "PLAIN-2550": "Barriers to Heart Disease Prevention", "PLAIN-2560": "Childhood Constipation and Cow\\u2019s Milk", "PLAIN-2570": "Diabetics Should Take Their Pulses", "PLAIN-2580": "Academy of Nutrition and Dietetics Conflicts of Interest", "PLAIN-2590": "Do Vegetarians Get Enough Protein?", "PLAIN-2600": "Eggs and Arterial Function", "PLAIN-2610": "Treating Asthma With Plants vs. Supplements?", "PLAIN-2620": "Phytates for the Treatment of Cancer", "PLAIN-2630": "Alkylphenol Endocrine Disruptors and Allergies", "PLAIN-2640": "Chicken Salmonella Thanks to Meat Industry Lawsuit", "PLAIN-2650": "Turmeric Curcumin and Osteoarthritis", "PLAIN-2660": "How Long to Detox From Fish Before Pregnancy?", "PLAIN-2670": "Is Caramel Color Carcinogenic?", "PLAIN-2680": "Counteracting the Effects of Dioxins Through Diet", "PLAIN-2690": "Chronic Headaches and Pork Tapeworms", "PLAIN-2700": "Heart Disease Starts in Childhood", "PLAIN-2710": "Artificial Food Colors and ADHD", "PLAIN-2720": "Keeping Your Hands Warm With Citrus", "PLAIN-2730": "Anti-Angiogenesis: Cutting Off Tumor Supply Lines", "PLAIN-2740": "Cancer Risk From CT Scan Radiation", "PLAIN-2750": "Preventing the Common Cold with Probiotics?", "PLAIN-2760": "Eating Healthy on a Budget", "PLAIN-2770": "Flaxseeds & Breast Cancer Survival: Clinical Evidence", "PLAIN-2780": "Do Fruit & Nut Bars Cause Weight Gain?", "PLAIN-2790": "Titanium Dioxide & Inflammatory Bowel Disease", "PLAIN-2800": "Prolonged Liver Function Enhancement From Broccoli", "PLAIN-2810": "Apple Juice May Be Worse Than Sugar Water", "PLAIN-2820": "Preventing Strokes with Diet", "PLAIN-2830": "Neurobiology of Artificial Sweeteners", "PLAIN-2840": "Benefits of Fenugreek Seeds", "PLAIN-2850": "More Antibiotics In White Meat or Dark Meat?", "PLAIN-2860": "BPA Plastic and Male Sexual Dysfunction", "PLAIN-2870": "Filled Full of Lead", "PLAIN-2880": "The Answer to the Pritikin Puzzle", "PLAIN-2890": "To Snack or Not to Snack?", "PLAIN-2900": "Boosting Good Bacteria in the Colon Without Probiotics", "PLAIN-2910": "Optimal Phytosterol Dose", "PLAIN-2920": "Human Neurotransmitters in Plants", "PLAIN-2930": "Kiwifruit for Irritable Bowel Syndrome", "PLAIN-2940": "Dietary Treatment of Crohn's Disease", "PLAIN-2950": "Unsafe at Any Feed", "PLAIN-2960": "Pharmacists Versus Health Food Store Employees: Who Gives Better Advice?", "PLAIN-2970": "Preventing Cataracts with Diet", "PLAIN-2981": "Cheese Mites and Maggots", "PLAIN-2991": "Cholesterol and Lower Back Pain", "PLAIN-3001": "EPIC Findings on Lymphoma", "PLAIN-3014": "Sometimes the Enzyme Myth Is True", "PLAIN-3026": "Vitamin C-Enriched Bacon", "PLAIN-3037": "Out of the Lab Onto the Track", "PLAIN-3053": "Dragon's Blood", "PLAIN-3063": "Better Than Goji Berries", "PLAIN-3074": "How to Help Prevent Abdominal Aortic Aneurysms", "PLAIN-3085": "The Difficulty of Arriving at a Vitamin D Recommendation", "PLAIN-3097": "Amyloid and Apple Juice", "PLAIN-3116": "Dietary Guidelines: From Dairies to Berries", "PLAIN-3131": "Are Avocados Good for You?", "PLAIN-3141": "Relieving Yourself of Excess Estrogen", "PLAIN-3151": "Too Much Iodine Can Be as Bad as Too Little", "PLAIN-3161": "Is Milk and Mucus a Myth?", "PLAIN-3171": "Convergence of Evidence", "PLAIN-3181": "Is Dragon Fruit Good For You?", "PLAIN-3191": "Is Distilled Fish Oil Toxin-Free?", "PLAIN-3201": "Acne & Cancer Connection", "PLAIN-3211": "Overdosing on Greens", "PLAIN-3221": "Dietary Theory of Alzheimer's", "PLAIN-3231": "Meat & Multiple Myeloma", "PLAIN-3241": "Apthous Ulcer Mystery Solved", "PLAIN-3251": "EPIC Study", "PLAIN-3261": "Update on Herbalife\\u00ae", "PLAIN-3271": "Saturated Fat & Cancer Progression", "PLAIN-3281": "Aluminum in Vaccines vs. Food", "PLAIN-3292": "Are Multivitamins Good For You?", "PLAIN-3302": "Fish Fog", "PLAIN-3312": "Sexually Transmitted Fish Toxin", "PLAIN-3322": "Veggies vs. Cancer", "PLAIN-3332": "Alcohol Risks vs. Benefits", "PLAIN-3342": "Is Coconut Milk Good For You?", "PLAIN-3352": "Boosting Heart Nerve Control", "PLAIN-3362": "Kuna Indian Secret", "PLAIN-3372": "The Healthiest Sweetener", "PLAIN-3382": "Are Artificial Colors Bad for You?", "PLAIN-3392": "Healthiest Airplane Beverage", "PLAIN-3402": "Antioxidant Content of 300 Foods", "PLAIN-3412": "Plant vs. Cow Calcium", "PLAIN-3422": "Vitamin Supplements Worth Taking", "PLAIN-3432": "Healthy Chocolate Milkshakes", "PLAIN-3442": "The Healthiest Vegetables", "PLAIN-3452": "Bowel Movement Frequency", "PLAIN-3462": "Olive Oil and Artery Function", "PLAIN-3472": "How Doctors Responded to Being Named a Leading Killer"}
"""


    static final String ME = 'beir.mailbox'

    static final Configuration config = new Configuration()

    static final PostOffice po = new PostOffice(config.EXCHANGE,config.HOST)

    MailBox box
    StopWords stopwords = new StopWords()
    Logger logger = LoggerFactory.getLogger("BEIR")
    String ID

    Map getDefaultParams(String domain = 'trec-covid') {
        return ["title-checkbox-1" : "1",
                      "title-weight-1" : "1.0",
                      "title-checkbox-2" : "2",
                      "title-weight-2" : "1.0",
                      "title-checkbox-3" : "3",
                      "title-weight-3" : "1.0",
                      "title-checkbox-4" : "4",
                      "title-weight-4" : "1.0",
                      "title-checkbox-5" : "5",
                      "title-weight-5" : "1.0",
                      "title-checkbox-6" : "6",
                      "title-weight-6" : "1.0",
                      "title-checkbox-7" : "7",
                      "title-weight-7" : "1.0",
                      "title-weight-x" : "0.9",
                      "abstract-checkbox-1" : "1",
                      "abstract-weight-1" : "1.0",
                      "abstract-checkbox-2" : "2",
                      "abstract-weight-2" : "1.0",
                      "abstract-checkbox-3" : "3",
                      "abstract-weight-3" : "1.0",
                      "abstract-checkbox-4" : "4",
                      "abstract-weight-4" : "1.0",
                      "abstract-checkbox-5" : "5",
                      "abstract-weight-5" : "1.0",
                      "abstract-checkbox-6" : "6",
                      "abstract-weight-6" : "1.0",
                      "abstract-checkbox-7" : "7",
                      "abstract-weight-7" : "1.0",
                      "abstract-weight-x" : "1.1",
                      "domain" : domain]

    }

    List<String> removeStopWords(Iterable<Token> tokens) {
        List<String> result = []
        tokens.each { Token token ->
            if (!stopwords.contains(token.word)) {
                result << token.word
            }
        }
        return result
    }

    double similarity(Iterable<String> left, Iterable<String> right) {
        HashMap<String, int[]> map = new HashMap<String, int[]>();
        left.each { String token ->
            String t = token.toLowerCase();
            if (!map.containsKey(t)) {
                map.put(t, new int[2]);
            }
            map.get(t)[0]++;
        }
        right.each { String token ->
            String t = token.toLowerCase();
            if (!map.containsKey(t)) {
                map.put(t, new int[2]);
            }
            map.get(t)[1]++;
        }
        double dot = 0;
        double norma = 0;
        double normb = 0;
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            int[] v = e.getValue();
            dot += v[0] * v[1];
            norma += v[0] * v[0];
            normb += v[1] * v[1];
        }
        norma = Math.sqrt(norma);
        normb = Math.sqrt(normb);
        if (dot == 0) {
            return 0;
        } else {
            return dot / (norma * normb);
        }
    }

    void run() {
        Map ID_doc_index = [:]
        Object lock = new Object()

        box = new MailBox(config.EXCHANGE, ME, config.HOST) {
            @Override
            void recv(String s){
                AskmeMessage message = Serializer.parse(s, AskmeMessage)
                Packet packet = message.getBody()
                packet.documents.each { Document doc ->
//                    double score = similarity(packet.query.terms, removeStopWords(doc.articleAbstract.tokens))
//                    println "${message.command}\t${doc.id}\t${doc.score}\t${score}" //"${doc.title.text}"
                    println "${message.command}\t${doc.id}\t${doc.score}" //"${doc.title.text}"
                }
                synchronized (lock) {
                    lock.notifyAll()
                }

            }
        }

        int size = 500  // Solr will return this many documents to rank.
        Map params = getDefaultParams(DOMAIN) // These never change
        JsonSlurper parser = new JsonSlurper()
        String questions = null

        if (DOMAIN == 'nfcorpus') {
            questions = NFCORPUS_QUESTIONS
        }
        else if (DOMAIN == 'trec-covid') {
            questions = TREC_COVID_QUESTIONS
        }
        else {
            throw new Exception("Invalid domain $DOMAIN")
        }
        def data = parser.parseText(questions)
//        for (int id = 1; id <= 2; ++id) {
//            String question = data.getAt("$id")
        data.each { id, question ->
            AskmeMessage message = new AskmeMessage()
            message.command = "$id"
            Packet packet = new Packet()
            packet.status = Status.OK
            packet.core = params.domain
            packet.query = new Query(question, size)
            message.setBody(packet)
            message.route(config.QUERY_MBOX)
            message.route(config.SOLR_MBOX)
            message.route(config.RANKING_MBOX)
            message.route(ME)
            message.setParameters(params)
            try {
                po.send(message)
            }
            finally {
                synchronized (lock) {
                    lock.wait()
                }
            }
//            println()
        }
        po.close()
        box.close()
    }

    static void main(String[] args) {
        new Beir().run()
    }
}


