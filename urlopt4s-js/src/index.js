import {
    RuleStorage,
    Engine,
    RequestType,
    Request,
    HTTPMethod,
    RemoveParamModifier,
    BufferRuleList
} from '@adguard/tsurlfilter';

let engine = null;

export const loadEngine = function(rules) {
    let ruleList = new BufferRuleList(1, rules, false);
    let ruleStorage = new RuleStorage([ ruleList ]);
    engine = new Engine(ruleStorage);
    engine.loadRules();
};

export const removeAdQueryParams = function(url) {
    let matchResult = engine.matchRequest(new Request(url, url, RequestType.Document, HTTPMethod.GET));
    let modifiedUrl = matchResult.getRemoveParamRules().reduce(function (url, rule) {
        if(rule.getAdvancedModifier() instanceof RemoveParamModifier) {
            let modifier = rule.getAdvancedModifier();
            return modifier.removeParameters(url);
        } else {
            return url;
        }
    }, url);
    return modifiedUrl;
};

