package me.botsko.prism.commands;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PrismApplierCallback;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Rollback;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;

import java.util.ArrayList;
import java.util.List;

public class RollbackCommand implements SubHandler {

	/**
	 * 
	 */
	private final Prism plugin;

	/**
	 * 
	 * @param plugin
	 * @return
	 */
	public RollbackCommand(Prism plugin) {
		this.plugin = plugin;
	}

	/**
	 * Handle the command
	 */
	@Override
	public void handle(final CallInfo call) {

		final QueryParameters parameters = PreprocessArgs.process(plugin, call.getSender(), call.getArgs(),
				PrismProcessType.ROLLBACK, 1, !plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
		if (parameters == null) {
			return;
		}
		parameters.setProcessType(PrismProcessType.ROLLBACK);
		parameters.setStringFromRawArgs(call.getArgs(), 1);

		// determine if defaults were used
		final ArrayList<String> defaultsUsed = parameters.getDefaultsUsed();
		StringBuilder defaultsReminder = new StringBuilder();
		if (!defaultsUsed.isEmpty()) {
			defaultsReminder.append(" using defaults:");
			for (final String d : defaultsUsed) {
				defaultsReminder.append(" ").append(d);
			}
		}

		call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Preparing results..." + defaultsReminder));

		/*
		  Run the query itself in an async task so the lookup query isn't done on the
		  main thread
		 */
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

			final ActionsQuery aq = new ActionsQuery(plugin);
			final QueryResult results = aq.lookup(parameters, call.getSender());
			if (!results.getActionResults().isEmpty()) {

				call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Beginning rollback..."));

				// Perform rollback on the main thread
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					final Rollback rb = new Rollback(plugin, call.getSender(), results.getActionResults(),
							parameters, new PrismApplierCallback());
					rb.apply();
				});

			} else {
				call.getSender().sendMessage(
						Prism.messenger.playerError("Nothing found to rollback. Try using /prism l (args) first."));
			}
		});
	}

	@Override
	public List<String> handleComplete(CallInfo call) {
		return PreprocessArgs.complete(call.getSender(), call.getArgs());
	}
}