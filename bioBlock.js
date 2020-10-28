const Block = require('./block.js');
const Blockchain = require('./blockchain.js');
const Calling = require('./callingJava.js');
module.exports = class BioBlock extends Block{
    // eslint-disable-next-line no-unused-vars
    constructor(rewardAddr, prevBlock, target=Blockchain.POW_TARGET, coinbaseReward=Blockchain.COINBASE_AMT_ALLOWED){
        super(rewardAddr, prevBlock);
        this.c = new Calling();
    }

    hasValidProof() {
        let score = this.c.alignScore(this.proof);
        return score >= this.c.baseScore + 100;
    }
};