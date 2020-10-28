const Miner = require('./miner.js');
const Calling = require('./callingJava.js');
const NUM_ROUNDS_MINING = 2000;
module.exports = class BioMiner extends Miner{
    // eslint-disable-next-line no-unused-vars
    constructor({name, net, startingBlock, miningRounds=NUM_ROUNDS_MINING} = {}) {
        super({name, net, startingBlock});
        this.c = new Calling();
    }

    findProof(oneAndDone=false) {
        for(let width of this.c.widths){
            this.currentBlock.proof = this.c.align(width);
            if (this.currentBlock.hasValidProof()) {
                this.log(`found proof for block ${this.currentBlock.chainLength}: ${this.currentBlock.proof}`);
                this.announceProof();
                this.receiveBlock(this.currentBlock);
                this.startNewSearch();
                break;
            }
        }
        // If we are testing, don't continue the search.
        if (!oneAndDone) {
            // Check if anyone has found a block, and then return to mining.
            setTimeout(() => this.emit(Miner.START_MINING), 0);
        }
    }

};