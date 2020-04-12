import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import * as API from '../api/API';
import { getCommand } from '../actions';
import { getQueryParamByName } from '../Utils';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import './Builder.scss';

class Builder extends Component {
    state = {
        userCommand: {
            flags: {},
            options: {}
        },
        saveInProgress: false
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const { match, location } = this.props;
        const userCommandId = getQueryParamByName('userCommandId', location.search);
        this.setState({ userCommandId });
        if (userCommandId != null) {
            this.props.getUserCommand(userCommandId).then((userCommand) => {
                this.setState({ userCommand });
                this.props.getCommand(match.params.commandName);
            });
        } else {
            this.props.getCommand(match.params.commandName);
        }
    }

    handleOptionChange(event) {
        const { name, value } = event.target;
        const userCommand = {
            flags: {
                ...this.state.userCommand.flags,
            },
            options: {
                ...this.state.userCommand.options,
                [name]: value
            }
        }

        this.setState({ userCommand });
    }

    handleFlagChange(event) {
        const { name, checked } = event.target;
        const userCommand = {
            flags: {
                ...this.state.userCommand.flags,
                [name]: checked
            },
            options: {
                ...this.state.userCommand.options
            }
        }

        this.setState({ userCommand });
    }

    getGeneratedFlags(command) {
        let flagsStr = '';
        let hyphenPrefixedFlags = '';
        let otherFlags = '';
        command.flags.filter(f => this.state.userCommand.flags[f.properties.name]).forEach(f => {
            if (f.properties.prefix === '-') {
                // normally only single hyphen prefixed flags are allowed to group
                hyphenPrefixedFlags += f.properties.name;
            } else {
                // double hyphen prefixed flags or non-prefixed flags are generally not grouped
                otherFlags += ` ${f.properties.prefix}${f.properties.name}`;
            }
        });

        hyphenPrefixedFlags = hyphenPrefixedFlags ? `-${hyphenPrefixedFlags.trim()}` : hyphenPrefixedFlags;
        otherFlags = otherFlags.trim();

        if (hyphenPrefixedFlags !== '' || otherFlags !== '') {
            flagsStr = `${hyphenPrefixedFlags} ${otherFlags}`;
        }
        return flagsStr;
    }

    getGeneratedOptions(command) {
        let optionsStr = '';
        command.options.filter(o => this.state.userCommand.options[o.properties.name]).forEach(o => {
            const prefix = !o.properties.prefix.endsWith('=') ? `${o.properties.prefix} ` : o.properties.prefix;
            optionsStr += ` ${prefix}${this.state.userCommand.options[o.properties.name]}`;
        });

        return optionsStr.trim();
    }

    getGeneratedCommand(command) {
        if (!command) return null;
        let commandStr = '';
        const flagsStr = this.getGeneratedFlags(command);
        const optionsStr = this.getGeneratedOptions(command);

        commandStr = `${command.properties.name} ${flagsStr} ${optionsStr}`;
        return commandStr;
    }

    handleSave(e) {
        e.preventDefault();
        const userCommand = {
            ...this.state.userCommand,
            name: this.props.command.properties.name,
            text: this.getGeneratedCommand(this.props.command),
            userId: this.props.user.localId,
            timestamp: Date.now()
        };
        this.setState({ saveInProgress: true });
        this.props.saveUserCommand(userCommand, this.state.userCommandId).then(() => {
            this.setState({ saveInProgress: false });
            this.props.history.push('/command/user-commands');
        });
    }

    render() {
        const { command, user } = this.props;
        const userCommand = this.state.userCommand || { flags: {}, options: {} };
        const newlineRegex = /(?:\r\n|\r|\n)/g;

        const generatedCommand = this.getGeneratedCommand(command);

        return command ? (
            <div className="builder">
                <div className="header">
                    <div className="text">
                        <code>
                            {generatedCommand != null && (<span>{generatedCommand}</span>)}
                        </code>
                    </div>
                </div>

                <div className="content">
                    <div className="title"><span>Build <span className="command-name">{command.properties.name}</span> Command</span></div>
                    <div className="category"><span>SYNTAX</span></div>
                    <div className="syntax"><code>
                        {command.properties.syntax.split(newlineRegex).map((item, index) => (
                            <span key={index}>{item.replace(/\.\.\./g, '···') /* replacing dots to avoid confusion with ellipsis */}<br /></span>
                        ))}</code><br />
                    </div>
                    <form onSubmit={(e) => this.handleSave(e)}>
                        <div className="section">
                            {command.options.length > 0 && (
                                <div className="options">
                                    <div className="category"><span>OPTIONS</span></div>
                                    <div className="fields">
                                        {command.options.map((option, i) => (
                                            <div key={i} className="row">
                                                <div className="label-col">
                                                    <label htmlFor={option.id}>{option.properties.desc}</label>
                                                </div>
                                                <div className="input-col">
                                                    <input id={option.id} type="text" name={option.properties.name}
                                                        onChange={(e) => this.handleOptionChange(e)}
                                                        value={userCommand.options[option.properties.name] || ''} />
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                    <br />
                                </div>
                            )}
                            {command.flags.length > 0 && (
                                <div className="flags">
                                    <div className="category"><span>FLAGS</span></div>
                                    <div className="fields">
                                        {command.flags.map((flag, i) => (
                                            <div key={i} className="row">
                                                <div className="label-col">
                                                    <label htmlFor={flag.id}>{flag.properties.desc}</label>
                                                </div>
                                                <div className="input-col">
                                                    <input id={flag.id} type="checkbox" name={flag.properties.name}
                                                        onChange={(e) => this.handleFlagChange(e)}
                                                        checked={!!userCommand.flags[flag.properties.name]} />
                                                    <label htmlFor={flag.id}>
                                                        <svg viewBox="0,0,50,50"><path d="M5 30 L 20 45 L 45 5"></path></svg>
                                                    </label>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                        <div className="form-buttons">
                            <button className="ripple" type="button">PRINT</button>
                            <button className="ripple tooltip-t"
                                {...(!user ? { 'data-tooltip': 'Login to Save' } : {})} type="submit"
                                disabled={!user || this.state.saveInProgress}>
                                {this.state.saveInProgress && <FontAwesomeIcon icon="circle-notch" spin />} SAVE
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        ) : '';
    }
}

const mapStateToProps = (state, props) => {
    return {
        command: state.commandReducer.command,
        user: state.authReducer.user
    }
}

const mapDispatchToProps = dispatch => {
    return {
        getCommand: (commandId) => {
            API.getCommand(commandId).then(command => dispatch(getCommand(command)));
        },
        getUserCommand: (userCommandId) => {
            return API.getUserCommand(userCommandId);
        },
        saveUserCommand: (userCommand, userCommandId) => { return API.saveUserCommand(userCommand, userCommandId); }
    }
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Builder));
