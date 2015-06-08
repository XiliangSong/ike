var React = require('react');
var bs = require('react-bootstrap');
var TableManager = require('../../managers/TableManager.js');
var DropdownButton = bs.DropdownButton
var Button = bs.Button
var MenuItem = bs.MenuItem
var Input = bs.Input
var ButtonGroup = bs.ButtonGroup
var xhr = require('xhr');

var SuggestQueryButton = React.createClass({

  getInitialState: function() {
    return {
      suggestions: [],
      disabled: false,
      narrow: false,
      waiting: false
    };
  },

  suggestQueryCallBack: function(err, resp, body) {
    this.setState({waiting: false})
    if (resp.statusCode == 200) {
      var newSuggestions = JSON.parse(body).scoredQueries
      this.setState({suggestions: newSuggestions})
    } else {
      alert('Got Error: ' + err + '\n<' + body + '>')
    }
  },

  suggestQuery: function() {
    var targetValue = this.props.target.value;
    if (targetValue === null) {
      alert('A target table must be set to use this feature')
      return;
    }

    var queryValue = this.props.query.value;
    if (queryValue === null) {
      alert('Enter a starting query');
      return;
    }

    var config = this.props.config.value.ml
    var requestConfig = {
      depth: config.depth,
      beamSize: config.beamSize,
      maxSampleSize: config.maxSampleSize,
      numEdits: config.numEdits,
      pWeight: config.pWeight,
      nWeight: config.nWeight,
      uWeight: config.uWeight,
      allowDisjunctions: config.allowDisjunctions
    }

    var tables = TableManager.getTables()
    var uri = this.props.makeUri('suggestQuery');
    var requestData = {
      body: JSON.stringify({
        query: queryValue,
        tables: tables,
        target: targetValue,
        narrow: this.state.narrow,
        config: requestConfig
      }),
      uri: uri,
      method: 'POST',
      headers: {'Content-Type': 'application/json'}
    };
    console.log('Requesting suggestions for ' + queryValue)
    this.setState({waiting: true})
    var request = xhr(requestData, this.suggestQueryCallBack);
  },
  suggestedQuerySelect: function(eventKey, href, target) {
    this.props.query.requestChange(target)
  },

  createMenuItem: function(scoredQuery) {
    return  (
            <MenuItem
              onSelect={this.suggestedQuerySelect}
              target={scoredQuery.query}>{scoredQuery.query}
            </MenuItem>
   )
  },

  checkBoxChange: function(event) {
    this.setState({narrow: !this.state.narrow})
  },

  smallFont: function(string) {
    return <div style={{fontSize: 'small'}}>{string}</div>
  },

  render: function() {
    return (
    <div>
      <label className="control-label">Query</label>
      <div>
        <ButtonGroup>
          <DropdownButton
            style={{fontSize: 'small'}}
            pullRight
            title="Suggestions"
            disabled={this.props.disabled}>
              {this.state.suggestions.map(this.createMenuItem)}
          </DropdownButton>
          <Button
            disabled={this.state.waiting || this.props.disabled}
            style={{fontSize: 'small'}}
            onClick={this.suggestQuery}
            >
              Refresh
          </Button>
        </ButtonGroup>
        <Input
          type='checkbox'
          style={{fontSize: 'small'}}
          label='Narrow'
          onChange={this.checkBoxChange}
          defaultChecked={this.state.narrow}
          disabled={this.props.disabled}/>
      </div>
    </div>
    );
  }
});
module.exports = SuggestQueryButton;