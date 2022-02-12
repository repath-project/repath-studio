# Context menu module

Based on reagent-contextmenu https://github.com/Frozenlock/reagent-contextmenu by Frozenlock under "Eclipse Public License" http://www.eclipse.org/legal/epl-v10.html

## Changes

- Split styles, views
- Use screenX/Y instead of clientX/Y to calculate the position of the menu

## Todo

- Move state to appDB an use subscriptions and event
- Split into two individual modules (overlay and menu) so we can reuse the overlay for different purposes