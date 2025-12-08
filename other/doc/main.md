# main
transforms a physical scene, actors, and input partials into early and late output partials per channel without exposing intermediate carriers.

## structures
~~~
reverb_context
  scene
  early_path_set
  early_statistics
  late_config

render_output
  early_partials
  late_partials
  residual_tails

## signatures
sp_reverb_build_scene ::
  geometry
  materials
  ->
  scene

sp_reverb_build_early ::
  scene
  source
  receiver
  ->
  early_path_set
  early_statistics

sp_reverb_build_late ::
  scene
  layout
  early_statistics
  ->
  late_config

sp_reverb_render_early ::
  scene
  early_path_set
  layout
  sampled_input_partials ...
  ->
  early_partials ...

sp_reverb_render_late ::
  late_config
  source_position
  layout
  sampled_input_partials ...
  ->
  late_partials ...
  residual_tails

sp_reverb_render ::
  scene
  source
  receiver
  layout
  sampled_input_partials ...
  ->
  render_output
~~~

## procedures
~~~
sp_reverb_build_scene
  treats geometry and materials as a complete description of the propagation domain.
  produces a scene that depends only on geometry and materials.
  stores geometric and spectral data in scene as immutable state for all subsequent uses.

sp_reverb_build_early
  treats scene, source, and receiver as a complete description of early geometric propagation.
  produces early_path_set as a purely geometric representation with no spectral dependence.
  derives early_statistics only from early_path_set and scene and from no other carrier.
  ensures that repeated calls with equal inputs yield geometrically equivalent early_path_set and equal early_statistics.

sp_reverb_build_late
  treats scene, layout, and early_statistics as the only inputs that influence late_config.
  produces late_config that is suitable for a stable feedback-based late field.
  ensures that changes in early_statistics that preserve energy, direction, and density invariants induce corresponding changes in late_config.
  leaves early_path_set and other early carriers without further role in late_config beyond what early_statistics encodes.

sp_reverb_render_early
  maps sampled_input_partials to early_partials by a linear operator for fixed scene, early_path_set, and layout.
  depends on scene and early_path_set only through geometric paths and material response.
  depends on layout only through the definition of receiver channels and their directional gains.
  produces early_partials that contain no dependence on late_config or modal quantities.

sp_reverb_render_late
  maps sampled_input_partials to late_partials and residual_tails by a linear operator for fixed late_config, source_position, and layout.
  depends on late_config only through its encoded modal and residual parameters.
  depends on layout only through receiver channel definitions and spatial embedding.
  produces late_partials and residual_tails that do not depend on early_path_set or other early carriers except through late_config.

sp_reverb_render
  treats scene, source, receiver, layout, and sampled_input_partials as a complete description of the rendered response.
  is linear in sampled_input_partials for fixed scene, source, receiver, and layout.
  is extensionally equivalent to combining an early-field mapping that satisfies sp_reverb_render_early with a late-field mapping that satisfies sp_reverb_render_late for compatible inputs.
  defines render_output so that early_partials and late_partials represent distinct additive contributions and residual_tails represent remaining late stochastic energy per channel.
~~~
